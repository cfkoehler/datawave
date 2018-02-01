package datawave.iterators;

import java.util.*;

import datawave.edge.protobuf.EdgeData;
import datawave.edge.protobuf.EdgeData.MetadataValue;
import datawave.edge.protobuf.EdgeData.MetadataValue.Metadata;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Combiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Combines edge metadata from different values.
 * 
 * This will always write protocol buffers as the value
 * 
 */
public class EdgeMetadataCombiner extends Combiner {
    
    private static final Logger log = LoggerFactory.getLogger(EdgeMetadataCombiner.class);
    
    /**
     * Reduces a list of Values into a single Value.
     *
     * @param key
     *            The most recent version of the Key being reduced.
     * @param iter
     *            An iterator over the Values for different versions of the key.
     * @return The combined Value.
     */
    @Override
    public Value reduce(Key key, Iterator<Value> iter) {
        
        // Since similar metadata values can have different date strings we only want to keep one
        // version of each piece of metadata along with its earliest start date.
        Map<Metadata,String> metadataFields = new TreeMap<>(new MetadataComparator());
        
        // possibly check that the key has "edge" as the cf?
        String earliestDate = null;
        
        while (iter.hasNext()) {
            
            Value value = iter.next();
            MetadataValue metadataVal;
            try {
                metadataVal = datawave.edge.protobuf.EdgeData.MetadataValue.parseFrom(value.get());
            } catch (InvalidProtocolBufferException e) {
                log.error("Found invalid Edge Metadata Value bytes.");
                continue;
            }
            
            List<Metadata> metadata = metadataVal.getMetadataList();
            
            // Go through and store the metadata with its earliest start date
            for (Metadata meta : metadata) {
                
                String tempDate = meta.getDate();
                
                if (!metadataFields.containsKey(meta)) {
                    metadataFields.put(meta, meta.getDate());
                } else if (metadataFields.get(meta).compareTo(meta.getDate()) > 0) {
                    metadataFields.put(meta, meta.getDate());
                }
                
            }
            
        }
        
        // Afterwards insert the earliest start date into its respective metadata object and add it to
        // the MetadataValueBuilder.
        EdgeData.MetadataValue.Metadata.Builder metaDatabuilder = MetadataValue.Metadata.newBuilder();
        MetadataValue.Builder builder = MetadataValue.newBuilder();
        for (Map.Entry<Metadata,String> metaEntry : metadataFields.entrySet()) {
            metaDatabuilder.clear();
            Metadata metadata = metaEntry.getKey();
            String date = metaEntry.getValue();
            
            metaDatabuilder.mergeFrom(metadata);
            metaDatabuilder.setDate(date);
            
            builder.addMetadata(metaDatabuilder);
        }
        
        return new Value(builder.build().toByteArray());
    }
    
    /*
     * Only compare the first 5 fields in the metedata Want to use this to match other metadata objects with the same fields but different dates 0 = they are
     * the same, this is good else = they are different, this is bad
     */
    private class MetadataComparator implements Comparator<Metadata> {
        @Override
        public int compare(Metadata m1, Metadata m2) {
            
            int jexl = metadataComparatorHelper(m1.getJexlPrecondition(), m2.getJexlPrecondition());
            
            int source = metadataComparatorHelper(m1.getSource(), m2.getSource());
            
            int sink = metadataComparatorHelper(m1.getSink(), m2.getSink());
            
            int enrich = metadataComparatorHelper(m1.getEnrichment(), m2.getEnrichment());
            
            int ei = metadataComparatorHelper(m1.getEnrichmentIndex(), m2.getEnrichmentIndex());
            
            return (jexl + source + sink + enrich + ei);
            
        }
        
        /*
         * 0 = they are the same, this is good 1 = they are different, this is bad
         */
        private int metadataComparatorHelper(String s1, String s2) {
            
            if (s1 != null && s2 != null) {
                if (s1.equals(s2)) {
                    return 0;
                } else {
                    return 1;
                }
            } else if (s1 == null && s2 == null) {
                return 0;
            } else {
                return 1;
            }
        }
    }
    
}
