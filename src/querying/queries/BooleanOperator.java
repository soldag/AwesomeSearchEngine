package querying.queries;

public enum BooleanOperator {
	And,
	Or,
	Not;
	
	
	/**
	 * Parses a string representing an boolean operator to the corresponding enum element.
	 * If string is no valid boolean operator, null is returned.
	 * @param operator
	 * @return
	 */
	public static BooleanOperator parse(String operator) {
		switch(operator.toUpperCase()) {
			case "AND":
				return BooleanOperator.And;
			
			case "OR":
				return BooleanOperator.Or;
				
			case "NOT":
				return BooleanOperator.Not;
				
			default:
				return null;
		}
	}
}
