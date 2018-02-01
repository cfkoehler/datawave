package datawave.query.config;

/**
 * This class represents a hole in the global index. Used by the PushdownMissingIndexRangeNodesVisitor.
 */
public class IndexHole implements Comparable<IndexHole> {
    private String startValue;
    private String endValue;
    private String startDate;
    private String endDate;
    
    public IndexHole() {}
    
    /**
     * Create an index with a date range and value range.
     * 
     * @param dateRange
     *            range in yyyyMMdd format
     * @param valueRange
     *            the start and end values of the known hole
     */
    public IndexHole(String[] dateRange, String[] valueRange) {
        setStartValue(valueRange[0]);
        setEndValue(valueRange[1]);
        setStartDate(dateRange[0]);
        setEndDate(dateRange[1]);
    }
    
    /**
     * Does the hole overlap the specified value
     */
    public boolean overlaps(String start, String end, String value) {
        if (startValue.compareTo(value) <= 0 && endValue.compareTo(value) >= 0) {
            if (startDate.compareTo(end) <= 0 && endDate.compareTo(start) >= 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Does the hole overlap the specified range.
     */
    public boolean overlaps(String start, String end, String lower, String upper) {
        if (startValue.compareTo(upper) <= 0 && endValue.compareTo(lower) >= 0) {
            if (startDate.compareTo(end) <= 0 && endDate.compareTo(start) >= 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Is this hole exist before the specified value.
     */
    public boolean before(String value) {
        return endValue.compareTo(value) < 0;
    }
    
    /**
     * Is this hole exist after the specified value.
     */
    public boolean after(String value) {
        return startValue.compareTo(value) > 0;
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append('[').append(startValue).append(',').append(endValue).append(']');
        builder.append(',');
        builder.append('[').append(startDate).append(',').append(endDate).append(']');
        builder.append(')');
        return builder.toString();
    }
    
    public boolean equals(Object o) {
        if (o instanceof IndexHole) {
            IndexHole hole = (IndexHole) o;
            return startValue.equals(hole.startValue) && endValue.equals(hole.endValue) && startDate.equals(hole.startDate) && endDate.equals(hole.endDate);
        }
        return false;
    }
    
    /**
     * Compare two holes. Must be sorted on value ranges first. PushdownMissingIndexRangeNodesVisitor depends on it.
     * 
     * @param hole
     * @return the comparison
     */
    public int compareTo(IndexHole hole) {
        int comparison = startValue.compareTo(hole.startValue);
        if (comparison == 0) {
            comparison = endValue.compareTo(hole.endValue);
            if (comparison == 0) {
                comparison = startDate.compareTo(hole.startDate);
                if (comparison == 0) {
                    comparison = endDate.compareTo(hole.endDate);
                }
            }
        }
        return comparison;
    }
    
    public String getStartValue() {
        return startValue;
    }
    
    public void setStartValue(String startValue) {
        this.startValue = startValue;
    }
    
    public String getEndValue() {
        return endValue;
    }
    
    public void setEndValue(String endValue) {
        this.endValue = endValue;
    }
    
    public String getStartDate() {
        return startDate;
    }
    
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    
    public String getEndDate() {
        return endDate;
    }
    
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
