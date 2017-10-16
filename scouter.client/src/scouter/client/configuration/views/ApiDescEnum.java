package scouter.client.configuration.views;

import org.eclipse.swt.SWT;

public enum ApiDescEnum {
	API("API", 250, SWT.LEFT, false),
	RETURN("RETURN", 50, SWT.LEFT, false),
	DESC("Description", 250, SWT.LEFT, false),;

	private final String title;
    private final int width;
    private final int alignment;
    private final boolean isNumber;

    private ApiDescEnum(String text, int width, int alignment, boolean isNumber) {
        this.title = text;
        this.width = width;
        this.alignment = alignment;
        this.isNumber = isNumber;
    }
    
    public String getTitle(){
        return title;
    }

    public int getAlignment(){
        return alignment;
    }

	public int getWidth() {
		return width;
	}
	
	public boolean isNumber() {
		return this.isNumber;
	}
}
