package scouter.client.configuration.views;

import org.eclipse.swt.SWT;

public enum ConfEnum {
	KEY("Key", 150, SWT.LEFT, false),
	VALUE("Value", 70, SWT.LEFT, false),
	DEFAULT("Def.", 70, SWT.LEFT, false);
	
	private final String title;
    private final int width;
    private final int alignment;
    private final boolean isNumber;

    private ConfEnum(String text, int width, int alignment, boolean isNumber) {
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
