package org.apache.jackrabbit.oak.benchmark.util;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.plugins.index.IndexConstants;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexEditorProvider;

/**
 * A simple utility class for Oak indexes.
 */
public class OakIndexUtils {
    
    /**
     * A property index
     */
    public static class PropertyIndex {
        
        private String indexName;
        
        private String propertyName;
        
        private String[] nodeTypeNames;

        /**
         * Set the index name. If not set, the index name is the property name.
         * 
         * @param indexName the index name
         * @return this
         */
        public PropertyIndex name(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * Set the property name. This field is mandatory.
         * 
         * @param propertyName the property name
         * @return this
         */
        public PropertyIndex property(String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        /**
         * Restrict the node types.
         * 
         * @param nodeTypeNames the list of declaring node types
         * @return this
         */
        public PropertyIndex nodeTypes(String... nodeTypeNames) {
            this.nodeTypeNames = nodeTypeNames;
            return this;
        }

        /**
         * Create the index.
         * <p>
         * If this is not a Oak repository, this method does nothing.
         * <p>
         * If a matching index already exists, this method verifies that the
         * definition matches. If no such index exists, a new one is created.
         * 
         * @param session the session to use for creating the index
         * @throws RepositoryException if writing to the repository failed, the
         *             index definition is incorrect, or if such an index exists
         *             but is not compatible with this definition (for example,
         *             a different property is indexed)
         */
        public void create(Session session) throws RepositoryException {
            if (!session.getWorkspace().getNodeTypeManager().hasNodeType(
                        "oak:queryIndexDefinition")) {
                // not an Oak repository
                return;
            }
            if (session.hasPendingChanges()) {
                throw new RepositoryException("The session has pending changes");
            }
            if (indexName == null) {
                indexName = propertyName;
            }
            if (propertyName == null) {
                throw new RepositoryException("Index property name not set");
            }
            if (nodeTypeNames != null) {
                if (nodeTypeNames.length == 0) {
                    // setting the node types to an empty array means "all node types"
                    // (same as not setting it)
                    nodeTypeNames = null;
                } else {
                    Arrays.sort(nodeTypeNames);
                }
            }
            Node root = session.getRootNode();
            Node indexDef;
            if (!root.hasNode(IndexConstants.INDEX_DEFINITIONS_NAME)) {
                indexDef = root.addNode(IndexConstants.INDEX_DEFINITIONS_NAME, 
                        JcrConstants.NT_UNSTRUCTURED);
                session.save();
            } else {
                indexDef = root.getNode(IndexConstants.INDEX_DEFINITIONS_NAME);
            }
            Node index;
            if (indexDef.hasNode(indexName)) {
                // verify the index matches
                index = indexDef.getNode(indexName);
                if (index.hasProperty(IndexConstants.UNIQUE_PROPERTY_NAME)) {
                    Property p = index.getProperty(IndexConstants.UNIQUE_PROPERTY_NAME);
                    if (p.getBoolean()) {
                        throw new RepositoryException(
                                "Index already exists, but is unique");
                    }
                }
                String type = index.getProperty(
                        IndexConstants.TYPE_PROPERTY_NAME).getString();
                if (!type.equals(PropertyIndexEditorProvider.TYPE)) {
                    throw new RepositoryException(
                            "Index already exists, but is of type " + type);
                }
                Value[] v = index.getProperty(IndexConstants.PROPERTY_NAMES).getValues();
                if (v.length != 1) {
                    String[] list = new String[v.length];
                    for (int i = 0; i < v.length; i++) {
                        list[i] = v[i].getString();
                    }
                    throw new RepositoryException(
                            "Index already exists, but is not just one property, but " + Arrays.toString(list));
                }
                if (!propertyName.equals(v[0].getString())) {
                    throw new RepositoryException(
                            "Index already exists, but is for property " + v[0].getString());
                }
                if (index.hasProperty(IndexConstants.DECLARING_NODE_TYPES)) {
                    v = index.getProperty(IndexConstants.DECLARING_NODE_TYPES).getValues();
                    String[] list = new String[v.length];
                    for (int i = 0; i < v.length; i++) {
                        list[i] = v[i].getString();
                    }
                    Arrays.sort(list);
                    if (Arrays.equals(list,  nodeTypeNames)) {
                        throw new RepositoryException(
                                "Index already exists, but with different node types: " + Arrays.toString(list));
                    }
                } else if (nodeTypeNames != null) {
                    throw new RepositoryException(
                            "Index already exists, but without node type restriction");
                }
                // matches
                return;
            }
            index = indexDef.addNode(indexName, IndexConstants.INDEX_DEFINITIONS_NODE_TYPE);
            index.setProperty(IndexConstants.TYPE_PROPERTY_NAME, 
                    PropertyIndexEditorProvider.TYPE);
            index.setProperty(IndexConstants.REINDEX_PROPERTY_NAME, 
                    true);
            index.setProperty(IndexConstants.PROPERTY_NAMES, 
                    new String[] { propertyName }, PropertyType.NAME);
            if (nodeTypeNames != null) {
                index.setProperty(IndexConstants.DECLARING_NODE_TYPES,
                        nodeTypeNames);
            }
            session.save();
        }
        
    }

}
