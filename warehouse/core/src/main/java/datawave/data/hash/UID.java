package datawave.data.hash;

import static datawave.data.hash.UIDConstants.CONFIG_MACHINE_ID_KEY;
import static datawave.data.hash.UIDConstants.CONFIG_UID_TYPE_KEY;
import static datawave.data.hash.UIDConstants.MILLISECONDS_PER_DAY;
import static datawave.data.hash.UIDConstants.DEFAULT_SEPARATOR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;

import datawave.util.StringUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.log4j.Logger;

/**
 * Internal, DATAWAVE-specific, unique identifier. Instead of using a UUID which consumes 128 bits, we are using:
 * 
 * originally: two concatenated int values that are results of computing a Murmur hash on the raw bytes using two different seeds. The resulting UID has the
 * form:
 * 
 * OptionPrefix.Hash1.Hash2
 * 
 * currently: three concatenated int values that are results of computing a Murmur hash on the raw bytes using three different seeds. The resulting UID has the
 * form:
 * 
 * Hash0.Hash1.Hash2
 * 
 * optionally: Additional data can be tagged onto the end
 * 
 * Hash0.Hash1.Hash2.Stuff
 * 
 */

public abstract class UID implements Comparable<UID>, Comparator<UID>, Writable {
    
    private static final Logger LOGGER = Logger.getLogger(UID.class);
    
    protected static final String[] EMPTY_EXTRAS = {};
    protected static final String NULL = "" + null;
    protected static final int RADIX = Character.MAX_RADIX;
    protected String optionalPrefix = null;
    protected String extra = null;
    
    /**
     * Empty constructor for using readFields
     */
    public UID() {}
    
    /**
     * Constructor
     * 
     * @param prefix
     *            Extra stuff to prepend to the UID, which can be null if specified as optional
     * @param isPrefixOptional
     *            if true, the prefix is optional and should not be validated for null
     * @param extras
     *            Extra stuff to append to the UID, which can be null
     */
    protected UID(final String prefix, boolean isPrefixOptional, final String... extras) {
        if (!isPrefixOptional && (null == prefix)) {
            throw new IllegalArgumentException("Prefix is not optional and must be non-null to use this constructor");
        }
        this.optionalPrefix = prefix;
        this.extra = mergeExtras(extras);
    }
    
    @Override
    public int compare(final UID o1, final UID o2) {
        return (null != o1) ? o1.compareTo(o2) : (null == o2) ? 0 : 1;
    }
    
    public abstract int compareTo(UID o);
    
    /**
     * Extract the number of milliseconds from the beginning of the day. Note that this is not entirely accurate but is significantly faster than using
     * GregorianCalendar operations.
     * 
     * @param time
     * @return the time % MILLISECONDS_PER_DAY, or -1 if time is null.
     */
    protected static int extractTimeOfDay(Date time) {
        if (time == null) {
            return -1;
        }
        return (int) (time.getTime() % MILLISECONDS_PER_DAY);
    }
    
    @Deprecated
    public String getOptionPrefix() {
        return optionalPrefix;
    }
    
    /**
     * 
     * Get the optional time component
     * 
     * @return The time component, -1 if not set
     */
    public abstract int getTime();
    
    /**
     * @return Any extra part of the UID (may be null)
     */
    public String getExtra() {
        return extra;
    }
    
    /**
     * Get the portion of the UID to be used for sharding (@see datawave.ingest.mapreduce.handler.shard.ShardIdFactory)
     * 
     * @return
     */
    public abstract String getShardedPortion();
    
    /**
     * Get the base portion of the uid (sans the extra part)
     * 
     * @return the base uid
     */
    public abstract String getBaseUid();
    
    /**
     * Combines multiple "extras" into a single extra
     * 
     * @param extras
     * @return merged extra
     */
    protected static String mergeExtras(final String... extras) {
        final String extra;
        if ((null != extras) && (extras.length > 0)) {
            if ((extras.length == 1) && (null == extras[0])) {
                extra = null;
            } else {
                final StringBuilder builder = new StringBuilder();
                for (int i = 0; i < extras.length; i++) {
                    if (i > 0) {
                        builder.append(DEFAULT_SEPARATOR);
                    }
                    builder.append(extras[i]);
                }
                
                extra = builder.toString();
            }
        } else {
            extra = null;
        }
        
        return extra;
    }
    
    /**
     * Creates a new UID builder based on default criteria
     * 
     * @return a default UID builder
     */
    public static UIDBuilder<UID> builder() {
        return HashUID.builder();
    }
    
    /**
     * Creates a new UID builder based on configured criteria, if defined
     * 
     * @param config
     *            Hadoop configuration, or null if a default builder is satisfactory
     * @return a builder based on configured or default criteria, as applicable
     */
    public static UIDBuilder<UID> builder(final Configuration config) {
        return builder(config, null);
    }
    
    /**
     * Creates a new UID builder based on default criteria and a date/time value, if defined
     * 
     * @return a default UID builder
     */
    public static UIDBuilder<UID> builder(final Date time) {
        return builder(null, time);
    }
    
    /**
     * Creates a new UID builder based on configured criteria and a date/time value, if defined
     * 
     * @param config
     * @param time
     *            a Date-based timestamp
     * @return a new UID builder
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static UIDBuilder<UID> builder(final Configuration config, final Date time) {
        // Declare the return value
        final UIDBuilder<UID> builder;
        
        // Create a builder based on the configured Snowflake type and machine ID, if applicable
        final String type = (null != config) ? config.get(CONFIG_UID_TYPE_KEY) : null;
        if (SnowflakeUID.class.getSimpleName().equals(type)) {
            int machineId = config.getInt(CONFIG_MACHINE_ID_KEY, -1);
            if (machineId >= 0) {
                if (config.getBoolean("snowflake.zookeeper.enabled", false)) {
                    ZkSnowflakeCache.init(config.get("snowflake.zookeepers"), config.getInt("snowflake.zk.init.retries", 5),
                                    config.getInt("snowflake.zk.init.sleep", 1000));
                } else {
                    LOGGER.warn("Attempting to generate snowflake ids without caching could cause uid collisions in the event of clock roll-back");
                }
                builder = (UIDBuilder) SnowflakeUID.builder(machineId);
            } else {
                final String message = "A 20-bit, non-negative, integer Machine ID must be configured with the " + CONFIG_MACHINE_ID_KEY
                                + " property key in order to build " + SnowflakeUID.class.getSimpleName() + "s";
                throw new IllegalArgumentException(message);
            }
        }
        // Otherwise, default to the hash-based builder
        else {
            builder = (UIDBuilder) ((null != time) ? HashUID.newBuilder(time) : HashUID.builder());
        }
        
        // Return the builder
        return builder;
    }
    
    /**
     * Parses the string representation of the hash
     * 
     * @param s
     * @return UID
     */
    public static <UID_TYPE extends UID> UID_TYPE parse(final String s) {
        return parse(s, -1);
    }
    
    /**
     * Parses the string representation of the hash, but only include up to maxExtraParts of the portion past the base hash
     * 
     * @param s
     * @param maxExtraParts
     *            is the number of pieces of the extra portion to include. -1 means all, 0 means none.
     * @return UID
     */
    @SuppressWarnings("unchecked")
    public static <UID_TYPE extends UID> UID_TYPE parse(final String s, int maxExtraParts) {
        if ((null == s) || s.isEmpty()) {
            throw new IllegalArgumentException("Not a valid " + UID.class.getSimpleName());
        }
        
        String[] parts = StringUtils.split(s, DEFAULT_SEPARATOR);
        
        final UID_TYPE uid;
        if (parts.length < 3) {
            uid = (UID_TYPE) SnowflakeUID.parse(s, maxExtraParts);
        } else {
            if (parts[0].length() > 8) {
                uid = (UID_TYPE) SnowflakeUID.parse(s, maxExtraParts);
            } else {
                uid = (UID_TYPE) HashUID.parse(s, maxExtraParts);
            }
        }
        
        return uid;
    }
    
    /**
     * Parses the string representation of the hash, but only the base part
     * 
     * @param s
     * @return UID
     */
    public static <UID_TYPE extends UID> UID_TYPE parseBase(String s) {
        return parse(s, 0);
    }
    
    public abstract void readFields(DataInput in) throws IOException;
    
    public void write(DataOutput out) throws IOException {
        out.writeUTF(toString());
    }
}
