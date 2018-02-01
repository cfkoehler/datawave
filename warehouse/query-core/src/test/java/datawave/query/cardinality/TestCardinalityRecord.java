package datawave.query.cardinality;

import com.vividsolutions.jts.util.Assert;
import org.junit.Test;

import java.util.*;

public class TestCardinalityRecord {
    
    @Test
    public void testCartesianProductOfFields1() {
        
        Set<String> recordedFields = new HashSet<>();
        CardinalityRecord cr = new CardinalityRecord(recordedFields, CardinalityRecord.DateType.DOCUMENT);
        
        Map<String,List<String>> valueMap = new HashMap<>();
        List<String> results = cr.assembleValues("FIELD1", valueMap);
        
        Assert.equals(0, results.size());
    }
    
    @Test
    public void testCartesianProductOfFields2() {
        
        Set<String> recordedFields = new HashSet<>();
        CardinalityRecord cr = new CardinalityRecord(recordedFields, CardinalityRecord.DateType.DOCUMENT);
        
        Map<String,List<String>> valueMap = new HashMap<>();
        List<String> list1 = new ArrayList<>();
        list1.add("L1V1");
        list1.add("L1V2");
        list1.add("L1V3");
        list1.add("L1V4");
        valueMap.put("FIELD1", list1);
        List<String> results = cr.assembleValues("FIELD1", valueMap);
        
        int expectedSize = list1.size();
        Assert.equals(expectedSize, results.size());
    }
    
    @Test
    public void testCartesianProductOfFields3() {
        
        Set<String> recordedFields = new HashSet<>();
        CardinalityRecord cr = new CardinalityRecord(recordedFields, CardinalityRecord.DateType.DOCUMENT);
        
        Map<String,List<String>> valueMap = new HashMap<>();
        List<String> list1 = new ArrayList<>();
        list1.add("L1V1");
        list1.add("L1V2");
        list1.add("L1V3");
        list1.add("L1V4");
        valueMap.put("FIELD1", list1);
        List<String> list2 = new ArrayList<>();
        list2.add("L2V1");
        list2.add("L2V2");
        list2.add("L2V3");
        list2.add("L2V4");
        valueMap.put("FIELD2", list2);
        List<String> results = cr.assembleValues("FIELD1|FIELD2", valueMap);
        
        int expectedSize = list1.size() * list2.size();
        Assert.equals(expectedSize, results.size());
    }
    
    @Test
    public void testCartesianProductOfFields4() {
        
        Set<String> recordedFields = new HashSet<>();
        CardinalityRecord cr = new CardinalityRecord(recordedFields, CardinalityRecord.DateType.DOCUMENT);
        
        Map<String,List<String>> valueMap = new HashMap<>();
        List<String> list1 = new ArrayList<>();
        list1.add("L1V1");
        list1.add("L1V2");
        list1.add("L1V3");
        list1.add("L1V4");
        valueMap.put("FIELD1", list1);
        List<String> list2 = new ArrayList<>();
        list2.add("L2V1");
        list2.add("L2V2");
        list2.add("L2V3");
        list2.add("L2V4");
        valueMap.put("FIELD2", list2);
        List<String> list3 = new ArrayList<>();
        list3.add("L3V1");
        list3.add("L3V2");
        valueMap.put("FIELD3", list3);
        List<String> list4 = new ArrayList<>();
        list4.add("L4V1");
        list4.add("L4V2");
        list4.add("L4V3");
        list4.add("L4V4");
        list4.add("L4V5");
        valueMap.put("FIELD4", list4);
        List<String> list5 = new ArrayList<>();
        list5.add("L5V1");
        list5.add("L5V2");
        list5.add("L5V3");
        list5.add("L5V4");
        valueMap.put("FIELD5", list5);
        List<String> results = cr.assembleValues("FIELD1|FIELD2|FIELD3|FIELD4|FIELD5", valueMap);
        
        int expectedSize = list1.size() * list2.size() * list3.size() * list4.size() * list5.size();
        Assert.equals(expectedSize, results.size());
    }
}
