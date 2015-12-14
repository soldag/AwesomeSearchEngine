package querying.queries;

public class BooleanQuery implements Query {
	
	/**
	 * Contains the type of the query.
	 */
	public static final String TYPE = "BOOLEAN";
	
	/**
	 * Contains the subquery on the left-hand side of the operator.
	 */
	private final Query leftQuery;
	
	/**
	 * Contains the subquery on the right-hand side of the operator.
	 */
	private final Query rightQuery;
	
	/**
	 * Contains the operator.
	 */
	private final BooleanOperator operator;
	
	
	/**
	 * Creates a new BooleanQuery instance.
	 * @param leftQuery
	 * @param rightQuery
	 * @param operator
	 */
	public BooleanQuery(Query leftQuery, Query rightQuery, BooleanOperator operator) {
		this.leftQuery = leftQuery;
		this.rightQuery = rightQuery;
		this.operator = operator;
	}
	
	
	/**
	 * Gets the subquery on the left-hand side of the operator.
	 * @return
	 */
	public Query getLeftQuery() {
		return this.leftQuery;
	}
	
	/**
	 * Gets the subquery on the right-hand side of the operator.
	 * @return
	 */
	public Query getRightQuery() {
		return this.rightQuery;
	}
	
	/**
	 * Gets the operator.
	 * @return
	 */
	public BooleanOperator getOperator() {
		return this.operator;
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public String toString() {
		return String.format("%s %s %s", this.getLeftQuery(), this.getOperator().toString().toUpperCase(), this.getRightQuery());
	}
}
