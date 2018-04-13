package query.analyzer.optimizer.logical;

import query.analyzer.exception.DNFOptimizeException;
import query.analyzer.exception.LogicalOptimizeException;
import query.analyzer.operator.FilterOperator;

import java.util.ArrayList;
import java.util.List;

import static com.corp.tsfile.qp.constant.SQLConstant.KW_AND;
import static com.corp.tsfile.qp.constant.SQLConstant.KW_OR;

public class DNFFilterOptimizer implements IFilterOptimizer {

    /**
     * get DNF(disjunctive normal form) for this filter operator tree. Before getDNF, this op tree
     * must be binary, in another word, each non-leaf node has exactly two children.
     * 
     * @return
     * @throws LogicalOptimizeException
     */
    @Override
    public FilterOperator optimize(FilterOperator filter) throws LogicalOptimizeException, DNFOptimizeException {
        return getDNF(filter);
    }

    private FilterOperator getDNF(FilterOperator filter) throws DNFOptimizeException {
        if (filter.isLeaf())
            return filter;
        List<FilterOperator> childOperators = filter.getChildren();
        if (childOperators.size() != 2) {
            throw new DNFOptimizeException("node :" + filter.getTokenSymbol() + " has "
                    + childOperators.size() + " childrean");

        }
        FilterOperator left = getDNF(childOperators.get(0));
        FilterOperator right = getDNF(childOperators.get(1));
        List<FilterOperator> newChildrenList = new ArrayList<FilterOperator>();
        switch (filter.getTokenIntType()) {
            case KW_OR:
                addChildOpInOr(left, newChildrenList);
                addChildOpInOr(right, newChildrenList);
                break;
            case KW_AND:
                if (left.getTokenIntType() != KW_OR && right.getTokenIntType() != KW_OR) {
                    addChildOpInAnd(left, newChildrenList);
                    addChildOpInAnd(right, newChildrenList);
                } else {
                    List<FilterOperator> leftAndChildren = getAndChild(left);
                    List<FilterOperator> rightAndChildren = getAndChild(right);
                    for (FilterOperator laChild : leftAndChildren) {
                        for (FilterOperator raChild : rightAndChildren) {
                            FilterOperator r = mergeToConjunction(laChild.clone(), raChild.clone());
                            newChildrenList.add(r);
                        }
                    }
                    filter.setTokenIntType(KW_OR);
                }
                break;
            default:
                throw new DNFOptimizeException("get DNF failed, this tokenType is:"
                        + filter.getTokenIntType());
        }
        filter.setChildrenList(newChildrenList);
        return filter;
    }

    /**
     * used by getDNF. merge two conjunction filter operators into a conjunction.<br>
     * conjunction operator consists of {@code FunctionOperator} and inner operator which token is
     * KW_AND.<br>
     * e.g. (a and b) merge (c) is (a and b and c)
     * 
     * @param b
     * @return
     * @throws DNFOptimizeException
     */
    private FilterOperator mergeToConjunction(FilterOperator a, FilterOperator b)
            throws DNFOptimizeException {
        List<FilterOperator> retChildrenList = new ArrayList<FilterOperator>();
        addChildOpInAnd(a, retChildrenList);
        addChildOpInAnd(b, retChildrenList);
        FilterOperator ret = new FilterOperator(KW_AND, false);
        ret.setChildrenList(retChildrenList);
        return ret;
    }

    /**
     * used by getDNF. get conjunction node. <br>
     * If child is basic function or AND node, return a list just containing this. <br>
     * If this child is OR, return children of OR.
     * 
     * @param child
     * @return
     */
    private List<FilterOperator> getAndChild(FilterOperator child) {
        switch (child.getTokenIntType()) {
            case KW_OR:
                return child.getChildren();
            default:
                // other token type means leaf node or and
                List<FilterOperator> ret = new ArrayList<FilterOperator>();
                ret.add(child);
                return ret;
        }
    }

    /**
     * used by getDNF
     * 
     * @param child
     * @param newChildrenList
     * @throws LogicalOptimizeException
     */
    private void addChildOpInAnd(FilterOperator child, List<FilterOperator> newChildrenList)
            throws DNFOptimizeException {
        if (child.isLeaf())
            newChildrenList.add(child);
        else if (child.getTokenIntType() == KW_AND)
            newChildrenList.addAll(child.getChildren());
        else {
            throw new DNFOptimizeException(
                    "add all children of an OR operator to newChildrenList in AND");
        }
    }

    /**
     * used by getDNF
     * 
     * @param child
     * @param newChildrenList
     */
    private void addChildOpInOr(FilterOperator child, List<FilterOperator> newChildrenList) {
        if (child.isLeaf() || child.getTokenIntType() == KW_AND)
            newChildrenList.add(child);
        else
            newChildrenList.addAll(child.getChildren());
    }

}
