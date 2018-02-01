package datawave.webservice.query.cachedresults;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.rowset.CachedRowSet;

import datawave.marking.MarkingFunctions;

import org.apache.log4j.Logger;

public class CacheableQueryRowReader {
    
    static private Logger log = Logger.getLogger(CacheableQueryRowReader.class);
    
    static public CacheableQueryRow createRow(CachedRowSet cachedRowSet, Set<String> fixedFieldsInEvent) {
        
        CacheableQueryRowImpl cqfc = new CacheableQueryRowImpl();
        
        ResultSetMetaData metadata;
        try {
            metadata = cachedRowSet.getMetaData();
            
            int numColumns = metadata.getColumnCount();
            Map<String,Integer> columnToIndexMap = new HashMap<String,Integer>();
            Map<String,Set<String>> columnValues = new HashMap<String,Set<String>>();
            Set<String> variableColumnNames = new TreeSet<String>();
            Set<String> fixedColumnNames = CacheableQueryRowImpl.getFixedColumnSet();
            // lets do a quick size estimate
            long characters = 0;
            for (int x = 1; x <= numColumns; x++) {
                String columnLabel = metadata.getColumnLabel(x);
                columnToIndexMap.put(columnLabel, x);
                String s = cachedRowSet.getString(x);
                if (s != null) {
                    characters += s.length();
                }
                if (fixedColumnNames.contains(columnLabel) == false || fixedFieldsInEvent.contains(columnLabel)) {
                    characters += columnLabel.length();
                    variableColumnNames.add(columnLabel);
                    if (s == null) {
                        columnValues.put(columnLabel, new LinkedHashSet<String>());
                    } else {
                        Set<String> columnValuesSet = new LinkedHashSet<String>();
                        columnValuesSet.add(s);
                        columnValues.put(columnLabel, columnValuesSet);
                    }
                }
            }
            
            // set the the size of the values in characters...internally converted to approximate bytes
            cqfc.setSizeInStoredCharacters(characters);
            
            cqfc.setVariableColumnNames(variableColumnNames);
            cqfc.setColumnValues(columnValues);
            
            if (columnToIndexMap.get("_user_") != null) {
                cqfc.setUser(cachedRowSet.getString(columnToIndexMap.get("_user_")));
            }
            if (columnToIndexMap.get("_queryId_") != null) {
                cqfc.setQueryId(cachedRowSet.getString(columnToIndexMap.get("_queryId_")));
            }
            if (columnToIndexMap.get("_logicName_") != null) {
                cqfc.setLogicName(cachedRowSet.getString(columnToIndexMap.get("_logicName_")));
            }
            if (columnToIndexMap.get("_datatype_") != null) {
                cqfc.setDataType(cachedRowSet.getString(columnToIndexMap.get("_datatype_")));
            }
            if (columnToIndexMap.get("_eventId_") != null) {
                cqfc.setEventId(cachedRowSet.getString(columnToIndexMap.get("_eventId_")));
            }
            if (columnToIndexMap.get("_row_") != null) {
                cqfc.setRow(cachedRowSet.getString(columnToIndexMap.get("_row_")));
            }
            if (columnToIndexMap.get("_colf_") != null) {
                cqfc.setColFam(cachedRowSet.getString(columnToIndexMap.get("_colf_")));
            }
            if (columnToIndexMap.get("_markings_") != null) {
                String mStr = cachedRowSet.getString(columnToIndexMap.get("_markings_"));
                cqfc.setMarkings(MarkingFunctions.Encoding.fromString(mStr));
            }
            if (columnToIndexMap.get("_column_markings_") != null) {
                String columnMarkings = cachedRowSet.getString(columnToIndexMap.get("_column_markings_"));
                Map<String,String> combinedColumnMarkings = parseColumnMarkings(columnMarkings, columnToIndexMap);
                Map<String,Map<String,String>> columnMarkingsMap = new HashMap<String,Map<String,String>>();
                Map<String,String> columnVisibilityMap = new HashMap<String,String>();
                for (Map.Entry<String,String> entry : combinedColumnMarkings.entrySet()) {
                    String columnName = entry.getKey();
                    String combinedString = entry.getValue();
                    int x = combinedString.lastIndexOf(":");
                    if (x >= 0) {
                        columnMarkingsMap.put(columnName, MarkingFunctions.Encoding.fromString(combinedString.substring(0, x)));
                        columnVisibilityMap.put(columnName, combinedString.substring(x + 1));
                    } else {
                        columnMarkingsMap.put(columnName, MarkingFunctions.Encoding.fromString(combinedString));
                        columnVisibilityMap.put(columnName, "");
                    }
                }
                cqfc.setColumnMarkingsMap(columnMarkingsMap);
                cqfc.setColumnColumnVisibilityMap(columnVisibilityMap);
            }
            if (columnToIndexMap.get("_column_timestamps_") != null) {
                String columnTimestamps = cachedRowSet.getString(columnToIndexMap.get("_column_timestamps_"));
                cqfc.setColumnTimestampMap(parseColumnTimestamps(columnTimestamps, columnToIndexMap));
            }
            
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        
        return cqfc;
    }
    
    static private Map<String,String> parseColumnMarkings(String s, Map<String,Integer> columnToIndexMap) {
        
        Map<Integer,String> indexToColumnMap = new HashMap<Integer,String>();
        for (Map.Entry<String,Integer> entry : columnToIndexMap.entrySet()) {
            indexToColumnMap.put(entry.getValue(), entry.getKey());
        }
        
        Map<String,String> colToMarkingMap = new HashMap<String,String>();
        
        String[] split1 = s.split("\0\0");
        for (String currSplit : split1) {
            String[] split2 = currSplit.split("\0");
            String marking = split2[0];
            String[] colNums = split2[1].split(",");
            for (String colNum : colNums) {
                Integer n = Integer.parseInt(colNum);
                String fieldName = indexToColumnMap.get(n);
                if (fieldName == null) {
                    colToMarkingMap.put("_DEFAULT_", marking);
                } else {
                    colToMarkingMap.put(indexToColumnMap.get(n), marking);
                }
            }
        }
        return colToMarkingMap;
    }
    
    static private Map<String,Long> parseColumnTimestamps(String s, Map<String,Integer> columnToIndexMap) {
        
        Map<Integer,String> indexToColumnMap = new HashMap<Integer,String>();
        for (Map.Entry<String,Integer> entry : columnToIndexMap.entrySet()) {
            indexToColumnMap.put(entry.getValue(), entry.getKey());
        }
        
        Map<String,Long> colToTimestampMap = new HashMap<String,Long>();
        
        String[] split1 = s.split("\0\0");
        for (String currSplit : split1) {
            String[] split2 = currSplit.split("\0");
            Long timestamp = Long.parseLong(split2[0]);
            String[] colNums = split2[1].split(",");
            for (String colNum : colNums) {
                Integer n = Integer.parseInt(colNum);
                String fieldName = indexToColumnMap.get(n);
                if (fieldName == null) {
                    colToTimestampMap.put("_DEFAULT_", timestamp);
                } else {
                    colToTimestampMap.put(indexToColumnMap.get(n), timestamp);
                }
            }
        }
        return colToTimestampMap;
    }
    
}
