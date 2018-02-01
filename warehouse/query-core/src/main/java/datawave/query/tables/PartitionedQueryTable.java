package datawave.query.tables;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import datawave.query.QueryParameters;
import datawave.query.planner.QueryPlanner;
import datawave.query.tables.chunk.Chunker;
import datawave.webservice.query.Query;
import datawave.webservice.query.QueryImpl.Parameter;
import datawave.webservice.query.configuration.GenericQueryConfiguration;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.log4j.Logger;

public class PartitionedQueryTable extends ShardQueryLogic {
    protected static final Logger log = Logger.getLogger(PartitionedQueryTable.class);
    
    private Connector connector;
    private Query settings;
    private Set<Authorizations> auths;
    
    private Chunker chunker;
    
    private QueryPlanner delegatePlanner = null;
    
    public PartitionedQueryTable() {
        super();
    }
    
    public PartitionedQueryTable(PartitionedQueryTable other) {
        super(other);
        this.setChunker(other.chunker.clone());
        this.setDelegateQueryPlanner(other.getDelegateQueryPlanner());
    }
    
    @Override
    public GenericQueryConfiguration initialize(Connector connection, Query settings, Set<Authorizations> auths) throws Exception {
        log.trace("initialize()");
        
        this.connector = connection;
        this.auths = auths;
        
        this.settings = settings.duplicate(settings.getQueryName() + "-chunk");
        chunker.setBaseQuery(this.settings);
        
        // this logic only accepts a list of selectors, possibly with ORs between them (simplified LUCENE)
        // but if the parent class thinks that the syntax is LUCENE, it will convert it to JEXL which screws up the chuncker
        Set<Parameter> params = new HashSet<>();
        Set<Parameter> origParams = this.settings.getParameters();
        for (Parameter p : origParams) {
            if (p.getParameterName().equals(QueryParameters.QUERY_SYNTAX) == false) {
                params.add(p);
            }
        }
        // ensure that the parent logic thinks that the syntax is JEXL so that it leaves it alone
        params.add(new Parameter(QueryParameters.QUERY_SYNTAX, "JEXL"));
        this.settings.setParameters(params);
        
        long maxConfiguredResults = 0;
        // General query options
        if (-1 == this.getMaxResults()) {
            maxConfiguredResults = Long.MAX_VALUE;
        } else {
            maxConfiguredResults = this.getMaxResults();
        }
        
        // Get the MAX_RESULTS_OVERRIDE parameter if given
        String maxResultsOverrideStr = settings.findParameter(QueryParameters.MAX_RESULTS_OVERRIDE).getParameterValue().trim();
        if (org.apache.commons.lang.StringUtils.isNotBlank(maxResultsOverrideStr)) {
            try {
                long override = Long.parseLong(maxResultsOverrideStr);
                if (override < maxConfiguredResults) {
                    this.setMaxResults(override);
                }
            } catch (NumberFormatException nfe) {
                log.error(QueryParameters.MAX_RESULTS_OVERRIDE + " query parameter is not a valid number: " + maxResultsOverrideStr + ", using default value");
            }
        }
        
        if (chunker.preInitializeQueryLogic()) {
            GenericQueryConfiguration config = super.initialize(this.connector, this.settings, this.auths);
            if (!config.getQueries().hasNext()) {
                return config;
            }
            
            chunker.initialize(config);
        }
        
        if (null != delegatePlanner) {
            setQueryPlanner(delegatePlanner);
        }
        
        return initializeNextChunk();
    }
    
    /**
     * This method will pull the next sub-query from the chunker and use that query to reinitialize the query logic.
     */
    public GenericQueryConfiguration initializeNextChunk() throws Exception {
        log.trace("initializeNextChunk()");
        if (chunker.hasNext()) {
            Query settings = chunker.next();
            return super.initialize(this.connector, settings, this.auths);
        } else {
            log.trace("Something went wrong in chunking...calling initialize with the original query");
            return super.initialize(this.connector, this.settings, this.auths);
        }
    }
    
    /**
     * Returns the iterator exposed by the ShardQueryLogic chain. This is used to construct the
     *
     * @return
     */
    private Iterator<Entry<Key,Value>> scanIterator() {
        return super.iterator();
    }
    
    /**
     * Returns an iterator to the results of every sub-query generated by the specified chunker.
     *
     * This iterator will re-initialize the query logic when moving between the generated query chunks.
     *
     */
    @Override
    public Iterator<Entry<Key,Value>> iterator() {
        return new Iterator<Entry<Key,Value>>() {
            Iterator<Entry<Key,Value>> currentChunk = scanIterator();
            
            @Override
            public boolean hasNext() {
                while (chunker.hasNext() && !currentChunk.hasNext()) {
                    try {
                        close();
                        GenericQueryConfiguration nextConfig = initializeNextChunk();
                        setupQuery(nextConfig);
                        this.currentChunk = scanIterator();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        log.warn("Could not process a chunk.", e);
                    }
                }
                return currentChunk.hasNext();
            }
            
            @Override
            public Entry<Key,Value> next() {
                return this.currentChunk.next();
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
    @Override
    public PartitionedQueryTable clone() {
        return new PartitionedQueryTable(this);
    }
    
    public Chunker getChunker() {
        return chunker;
    }
    
    public void setChunker(Chunker chunker) {
        this.chunker = chunker;
    }
    
    public QueryPlanner getDelegateQueryPlanner() {
        return delegatePlanner;
    }
    
    public void setDelegateQueryPlanner(QueryPlanner planner) {
        this.delegatePlanner = planner;
    }
}
