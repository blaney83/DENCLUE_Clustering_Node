package io.github.blaney83.dencluecluster;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "DENCLUECluster" Node.
 * Clusters data using the DENCLUE algorithm. This density-based method used Gaussian distribution and locates local maxima using hill-climbing. Data is pre-processed into grid cells (using a variation of the OptiGrid approach) and the summation of maxima is restricted to neighboring cells keep runtime low. For this reason, the DENCLUE method is faster than DBScan and OPTICS algorithms.
 *
 * @author Benjamin Laney
 */
public class DENCLUEClusterNodeView extends NodeView<DENCLUEClusterNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link DENCLUEClusterNodeModel})
     */
    protected DENCLUEClusterNodeView(final DENCLUEClusterNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        DENCLUEClusterNodeModel nodeModel = 
            (DENCLUEClusterNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

