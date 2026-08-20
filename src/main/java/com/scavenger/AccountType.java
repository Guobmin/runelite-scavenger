package com.scavenger;

public enum AccountType
{
	FREE("Free-to-play"),
	MEMBERS("Members");

	private final String label;

	AccountType(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
