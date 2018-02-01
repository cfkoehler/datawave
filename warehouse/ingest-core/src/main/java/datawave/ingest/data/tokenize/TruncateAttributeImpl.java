package datawave.ingest.data.tokenize;

import org.apache.lucene.util.AttributeImpl;

public class TruncateAttributeImpl extends AttributeImpl implements TruncateAttribute {
    
    boolean truncated;
    int originalLength;
    
    @Override
    public boolean isTruncated() {
        // TODO Auto-generated method stub
        return truncated;
    }
    
    @Override
    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }
    
    @Override
    public void clear() {
        this.truncated = false;
    }
    
    @Override
    public void copyTo(AttributeImpl arg0) {
        TruncateAttribute a = (TruncateAttribute) arg0;
        a.setTruncated(this.isTruncated());
    }
    
    @Override
    public int getOriginalLength() {
        // TODO Auto-generated method stub
        return originalLength;
    }
    
    @Override
    public void setOriginalLength(int len) {
        this.originalLength = len;
    }
    
}
