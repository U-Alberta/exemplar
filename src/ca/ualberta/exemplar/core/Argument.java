package ca.ualberta.exemplar.core;

public class Argument {
	
	String argumentType;
	String entityId;
	String entityName;
	String entityType;
	
	// Token indexes
	private int startIndex, endIndex;
	
	// Character offsets
	private int startOffset, endOffset;
	
	public Argument(String argumentType, String entityId, String entityName, String entityType, int startIndex, int endIndex, int startOffset, int endOffset){
		this.entityId = entityId;
		this.entityType = entityType;
		this.entityName = entityName;
		this.argumentType = argumentType;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}
	
	public String getArgumentType() {
		return argumentType;
	}
	public void setArgumentType(String argumentType) {
		this.argumentType = argumentType;
	}
	
	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entity) {
		this.entityName = entity;
	}
	
	
	public int getStartIndex() {
		return startIndex;
	}
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}
	
	public int getEndIndex() {
		return endIndex;
	}
	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}
	
	public int getStartOffset() {
		return startOffset;
	}
	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}
	
	public int getEndOffset() {
		return endOffset;
	}
	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}

	@Override
	public String toString(){
		return entityName;
	}

}
