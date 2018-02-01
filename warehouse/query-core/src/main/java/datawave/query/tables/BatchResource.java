package datawave.query.tables;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import datawave.security.util.ScannerHelper;

import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class BatchResource extends RunningResource {
    
    private static final Logger log = Logger.getLogger(BatchResource.class);
    
    protected BatchResource(Connector cxn) {
        super(cxn);
    }
    
    public BatchResource(AccumuloResource copy) {
        super(copy);
    }
    
    /**
     * Initializes the scanner resource
     * 
     * @param auths
     * @param tableName
     * @throws TableNotFoundException
     * 
     */
    @Override
    protected void init(final String tableName, final Set<Authorizations> auths, Collection<Range> currentRange) throws TableNotFoundException {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkArgument(null != currentRange && currentRange.size() > 0);
        
        // copy the appropriate variables.
        ranges = Lists.newArrayList(currentRange);
        
        this.tableName = tableName;
        
        this.auths = Sets.newHashSet(auths);
        
        if (log.isTraceEnabled())
            log.trace("Creating scanner resource from " + tableName + " " + auths + " " + currentRange);
        
        internalTimer = new StopWatch();
        internalTimer.start();
        
        // let's pre-compute the hashcode.
        hashCode += new HashCodeBuilder().append(tableName).append(auths).append(ranges).toHashCode();
        
        baseScanner = ScannerHelper.createBatchScanner(getConnector(), tableName, auths, 2);
        
        if (baseScanner != null) {
            ((BatchScanner) baseScanner).setRanges(currentRange);
        }
        
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        
        if (null != baseScanner) {
            if (log.isDebugEnabled()) {
                log.debug("Closing " + toString());
                
            }
            baseScanner.close();
        }
        baseScanner = null;
    }
}
