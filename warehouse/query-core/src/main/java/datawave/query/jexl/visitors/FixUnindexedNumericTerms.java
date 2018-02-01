package datawave.query.jexl.visitors;

import java.util.Collection;

import datawave.data.type.Type;
import datawave.query.config.ShardQueryConfiguration;
import datawave.query.exceptions.DatawaveFatalQueryException;
import datawave.query.jexl.JexlASTHelper;
import datawave.query.jexl.JexlASTHelper.IdentifierOpLiteral;
import datawave.query.jexl.JexlNodeFactory;
import datawave.webservice.common.logging.ThreadConfigurableLogger;
import datawave.webservice.query.exception.DatawaveErrorCode;
import datawave.webservice.query.exception.QueryException;

import org.apache.commons.jexl2.parser.ASTEQNode;
import org.apache.commons.jexl2.parser.ASTERNode;
import org.apache.commons.jexl2.parser.ASTGENode;
import org.apache.commons.jexl2.parser.ASTGTNode;
import org.apache.commons.jexl2.parser.ASTLENode;
import org.apache.commons.jexl2.parser.ASTLTNode;
import org.apache.commons.jexl2.parser.ASTNENode;
import org.apache.commons.jexl2.parser.ASTNRNode;
import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

/**
 * When we have an unindexed field that appears numeric in nature, then convert the string literals to numeric literals.
 *
 */
public class FixUnindexedNumericTerms extends RebuildingVisitor {
    private static final Logger log = ThreadConfigurableLogger.getLogger(FixUnindexedNumericTerms.class);
    
    private final ShardQueryConfiguration config;
    
    public FixUnindexedNumericTerms(ShardQueryConfiguration config) {
        Preconditions.checkNotNull(config);
        
        this.config = config;
    }
    
    /**
     * Change string literals to numeric literals for comparisons with unindexed fields (where possible)
     *
     * @param config
     * @param script
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends JexlNode> T fixNumerics(ShardQueryConfiguration config, T script) {
        FixUnindexedNumericTerms visitor = new FixUnindexedNumericTerms(config);
        
        if (null == visitor.config.getQueryFieldsDatatypes()) {
            QueryException qe = new QueryException(DatawaveErrorCode.DATATYPESFORINDEXFIELDS_MULTIMAP_MISSING);
            throw new DatawaveFatalQueryException(qe);
        }
        
        return (T) script.jjtAccept(visitor, null);
    }
    
    @Override
    public Object visit(ASTEQNode node, Object data) {
        return expandNodeForNormalizers(node, data);
    }
    
    @Override
    public Object visit(ASTNENode node, Object data) {
        return expandNodeForNormalizers(node, data);
    }
    
    @Override
    public Object visit(ASTERNode node, Object data) {
        return expandNodeForNormalizers(node, data);
    }
    
    @Override
    public Object visit(ASTNRNode node, Object data) {
        return expandNodeForNormalizers(node, data);
    }
    
    @Override
    public Object visit(ASTLTNode node, Object data) {
        return expandNodeForNormalizers(node, data);
    }
    
    @Override
    public Object visit(ASTLENode node, Object data) {
        return expandNodeForNormalizers(node, data);
    }
    
    @Override
    public Object visit(ASTGTNode node, Object data) {
        return expandNodeForNormalizers(node, data);
    }
    
    @Override
    public Object visit(ASTGENode node, Object data) {
        return expandNodeForNormalizers(node, data);
    }
    
    protected JexlNode expandNodeForNormalizers(JexlNode node, Object data) {
        IdentifierOpLiteral op = JexlASTHelper.getIdentifierOpLiteral(node);
        if (op == null) {
            return node;
        }
        
        final String fieldName = op.deconstructIdentifier();
        Object literal = op.getLiteralValue();
        
        // Get all of the normalizers for the field name
        Collection<Type<?>> normalizers = config.getQueryFieldsDatatypes().get(fieldName);
        
        // Catch the case of the user entering FIELD == null
        if (normalizers.isEmpty() && literal instanceof String) {
            try {
                literal = NumberUtils.createNumber((String) literal);
            } catch (Exception nfe) {
                // leave literal as is
            }
        }
        
        // Return a copy of the node with a potentially converted literal
        return JexlNodeFactory.buildUntypedNewLiteralNode(node, fieldName, literal);
    }
    
}
