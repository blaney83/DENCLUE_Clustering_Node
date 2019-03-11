package io.github.blaney83.dencluecluster;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "DENCLUECluster" Node.
 * Clusters data using the DENCLUE algorithm. This density-based method used Gaussian distribution and locates local maxima using hill-climbing. Data is pre-processed into grid cells (using a variation of the OptiGrid approach) and the summation of maxima is restricted to neighboring cells keep runtime low. For this reason, the DENCLUE method is faster than DBScan and OPTICS algorithms.
 *
 * @author Benjamin Laney
 */
public class DENCLUEClusterNodeFactory 
        extends NodeFactory<DENCLUEClusterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DENCLUEClusterNodeModel createNodeModel() {
        return new DENCLUEClusterNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<DENCLUEClusterNodeModel> createNodeView(final int viewIndex,
            final DENCLUEClusterNodeModel nodeModel) {
        return new DENCLUEClusterNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new DENCLUEClusterNodeDialog();
    }

}

