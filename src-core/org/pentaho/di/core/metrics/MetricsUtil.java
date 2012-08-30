package org.pentaho.di.core.metrics;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.logging.Metrics;
import org.pentaho.di.core.logging.MetricsInterface;
import org.pentaho.di.core.logging.MetricsRegistry;

public class MetricsUtil {

  /**
   * Calculates the durations between the START and STOP snapshots for a given metric description
   * @param logChannelId the id of the log channel to investigate
   * @param metricsCode the metric code
   * @return the duration in ms
   */
  public static List<MetricsDuration> getDuration(String logChannelId, Metrics metric) {
    List<MetricsDuration> durations = new ArrayList<MetricsDuration>();
    
    Deque<MetricsSnapshotInterface> metrics = MetricsRegistry.getInstance().getSnapshotList(logChannelId);
    MetricsSnapshotInterface start=null;
    
    Iterator<MetricsSnapshotInterface> iterator = metrics.iterator();
    while (iterator.hasNext()) {
      MetricsSnapshotInterface snapshot = iterator.next();
      if (snapshot.getMetric().equals(metric)) {
        if (snapshot.getMetric().getType() == MetricsSnapshotType.START) {
          if (start!=null) {
            // We didn't find a stop for the previous start so add it with a null duration
            durations.add(new MetricsDuration(start.getDate(), snapshot.getMetric().getDescription(), snapshot.getSubject(), logChannelId, null));
          }
          start = snapshot;
        } else {
          long duration = snapshot.getDate().getTime() - start.getDate().getTime();
          durations.add(new MetricsDuration(start.getDate(), snapshot.getMetric().getDescription(), snapshot.getSubject(), logChannelId, duration));
          start=null;
        }
      }
    }
    
    // Now aggregate even further, calculate total times...
    //
    Map<String, MetricsDuration> map = new HashMap<String, MetricsDuration>();
    for (MetricsDuration duration : durations) {
      String key = duration.getSubject()==null ? duration.getDescription() : duration.getDescription()+" / "+duration.getSubject();
      MetricsDuration agg = map.get(key);
      if (agg==null) {
        map.put(key, duration);
      } else {
        agg.setDuration(agg.getDuration()+duration.getDuration());
      }
    }
    
    // If we already have 
    
    return new ArrayList<MetricsDuration>( map.values() );
  }
  
  /**
   * Calculates the durations between the START and STOP snapshots per metric description and subject (if any)
   * @param logChannelId the id of the log channel to investigate
   * @return the duration in ms
   */
  public static List<MetricsDuration> getDurations(String logChannelId) {
    Map<String, MetricsSnapshotInterface> last = new HashMap<String, MetricsSnapshotInterface>();
    Map<String, MetricsDuration> map = new HashMap<String, MetricsDuration>();
    
    Deque<MetricsSnapshotInterface> metrics = MetricsRegistry.getInstance().getSnapshotList(logChannelId);
    
    Iterator<MetricsSnapshotInterface> iterator = metrics.iterator();
    while (iterator.hasNext()) {
      MetricsSnapshotInterface snapshot = iterator.next();
      
      // Do we have a start point in the map?
      //
      String key = snapshot.getMetric().getDescription()+(snapshot.getSubject()==null ? "" : (" - "+snapshot.getSubject()));
      MetricsSnapshotInterface lastSnapshot = last.get(key);
      if (lastSnapshot==null) {
        lastSnapshot = snapshot;
      } else {
        // If we have a START-STOP range, calculate the duration and add it to the duration map...
        //
        MetricsInterface metric = lastSnapshot.getMetric();
        if (metric.getType()==MetricsSnapshotType.START && metric.getType()==MetricsSnapshotType.STOP) {
          long extraDuration = snapshot.getDate().getTime() - lastSnapshot.getDate().getTime();
          
          MetricsDuration metricsDuration = map.get(key);
          if (metricsDuration==null) {
            metricsDuration = new MetricsDuration(lastSnapshot.getDate(), metric.getDescription(), lastSnapshot.getSubject(), logChannelId, extraDuration);
          } else {
            metricsDuration.setDuration(metricsDuration.getDuration()+extraDuration);
            metricsDuration.incrementCount();
          }
        }
      }
      last.put(key, lastSnapshot);
    }
    
    return new ArrayList<MetricsDuration>( map.values() );
  }

  public static List<MetricsSnapshotInterface> getResultsList(Metrics metric) {
    List<MetricsSnapshotInterface> snapshots = new ArrayList<MetricsSnapshotInterface>();

    Map<String, Map<String, MetricsSnapshotInterface>> snapshotMaps = MetricsRegistry.getInstance().getSnapshotMaps();
    Iterator<Map<String, MetricsSnapshotInterface>> mapsIterator = snapshotMaps.values().iterator();
    while (mapsIterator.hasNext()) {
      Map<String, MetricsSnapshotInterface> map = mapsIterator.next();
      Iterator<MetricsSnapshotInterface> snapshotIterator = map.values().iterator();
      while (snapshotIterator.hasNext()) {
        MetricsSnapshotInterface snapshot = snapshotIterator.next();
        if (snapshot.getMetric().equals(metric)) {
          snapshots.add(snapshot);
        }
      }
    }
    
    return snapshots;
  }
  
  public static Long getResult(Metrics metric) {
    Map<String, Map<String, MetricsSnapshotInterface>> snapshotMaps = MetricsRegistry.getInstance().getSnapshotMaps();
    Iterator<Map<String, MetricsSnapshotInterface>> mapsIterator = snapshotMaps.values().iterator();
    while (mapsIterator.hasNext()) {
      Map<String, MetricsSnapshotInterface> map = mapsIterator.next();
      Iterator<MetricsSnapshotInterface> snapshotIterator = map.values().iterator();
      while (snapshotIterator.hasNext()) {
        MetricsSnapshotInterface snapshot = snapshotIterator.next();
        if (snapshot.getMetric().equals(metric)) {
          return snapshot.getValue();
        }
      }
    }
    
    return null;
  }

}