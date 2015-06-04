package org.csstudio.swt.xygraph.linearscale;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.eclipse.draw2d.Figure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;


/**
 * The abstract scale has the common properties for linear(straight) scale and 
 * round scale.
 * @author Xihui Chen
 *
 */
public abstract class AbstractScale extends Figure{	
	

    /** ticks label's position relative to tick marks*/
    public enum LabelSide {

        /** bottom or left side of tick marks for linear scale, 
         *  or outside for round scale */
        Primary,

        /** top or right side of tick marks for linear scale, 
         *  or inside for round scale*/
        Secondary
    }


	public static final double DEFAULT_MAX = 100d;


	public static final double DEFAULT_MIN = 0d;


	public static final String DEFAULT_ENGINEERING_FORMAT = "0.####E0";//$NON-NLS-1$


	/**
	 * the digits limit to be displayed in engineering format
	 */
	private static final int ENGINEERING_LIMIT = 4;

	private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd\nHH:mm:ss";    	//$NON-NLS-1$
    
    /** ticks label position */
    private LabelSide tickLableSide = LabelSide.Primary;   


    /** the default minimum value of log scale range */
    public final static double DEFAULT_LOG_SCALE_MIN = 0.1d;

    /** the default maximum value of log scale range */
    public final static double DEFAULT_LOG_SCALE_MAX = 100d;
    
    /** the default label format */
    private String default_decimal_format = "############.##";    //$NON-NLS-1$
	
    /** the state if the axis scale is log scale */
    protected boolean logScaleEnabled = false;
    
	/** The minimum value of the scale */
    protected double min = DEFAULT_MIN;
	
	/** The maximum value of the scale */
	protected double max = DEFAULT_MAX;	

	 /** the format for tick labels */
     private String formatPattern;
    
    /** the time unit for tick step */
    private int timeUnit = 0;
    
     /** Whenever any parameter has been  changed, the scale should be marked as dirty, 
      * so all the inner parameters could be recalculated before the next paint*/
    protected boolean dirty = true;

    private boolean dateEnabled = false;
    
    private boolean scaleLineVisible = true;

	/** the pixels hint for major tick mark step */
    private int majorTickMarkStepHint = 30;
    
    /** the pixels hint for minor tick mark step */
    private int minorTickMarkStepHint = 4;
    
    private boolean minorTicksVisible= true;
    
    private double majorGridStep = 0;

    private boolean autoFormat = true;
	
    private Range range = new Range(min, max);
    
    
    
	/**
     * Formats the given object.
     * 
     * @param obj
     *            the object
     * @return the formatted string
     */
    public String format(Object obj) {
    	return format(obj, false);
    }
    
	/**
     * Formats the given object.
     * 
     * @param obj
     *            the object
     * @param minOrMaxDate
     * 			true if it is the min or max date on the scale.
     * @return the formatted string
     */
    public String format(Object obj, boolean minOrMaxDate) {     
        	
            if (isDateEnabled()) {
              	if (autoFormat || formatPattern == null || formatPattern.equals("")
            			|| formatPattern.equals(default_decimal_format)
            			|| formatPattern.equals(DEFAULT_ENGINEERING_FORMAT)) {            		
            		formatPattern =  DEFAULT_DATE_FORMAT;     
            		double length = Math.abs(max - min);            		
	                if (length <=5000 || timeUnit == Calendar.MILLISECOND) { //less than five second
	                	formatPattern = "ss.SSS";//$NON-NLS-1$
	                } else if (length <=1800000d || timeUnit == Calendar.SECOND) { //less than 30 min
	                	formatPattern = "HH:mm:ss";//$NON-NLS-1$
	                } else if (length <= 86400000d || timeUnit == Calendar.MINUTE) { // less than a day
	                	formatPattern = "HH:mm";//$NON-NLS-1$
	                } else if (length <= 604800000d || timeUnit == Calendar.HOUR_OF_DAY) { //less than a week
	                	formatPattern = "MM-dd\nHH:mm";//$NON-NLS-1$
	                } else if (length <= 2592000000d || timeUnit == Calendar.DATE) { //less than a month
	                	formatPattern = "MM-dd";//$NON-NLS-1$
//	                } else if (length <= 31536000000d ||timeUnit == Calendar.MONTH) { //less than a year
//	                	formatPattern = "yyyy-MM-dd";//$NON-NLS-1$
	                } else {		//more than a month
	                	formatPattern = "yyyy-MM-dd"; //$NON-NLS-1$
	                } 
	                autoFormat = true;
            	}
              	if(minOrMaxDate && autoFormat){
              			if(Math.abs(max - min)<5000)
              				return new SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss.SSS").format(obj); //$NON-NLS-1$
           			return new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(obj);
              	}
            	return new SimpleDateFormat(formatPattern).format(obj);
            }
            
            if (formatPattern == null || formatPattern.equals("")) {            	
            	formatPattern = default_decimal_format;  
            	autoFormat = true;
            }       
            
            // Edited by scouter.project@gmail.com
            //return new DecimalFormat(formatPattern).format(obj);
            String value = new DecimalFormat(formatPattern).format(obj);
            if (decreaseValue) {
            	return formatDecreasedValue(value);
            }
            return value;
   }
    
    // ***************Added by scouter.project@gmail.com *****************
    
    /**
     * 1,000 -> 1K
     * 1,000,000 -> 1M
     * 1,000,000,000 -> 1G
     * 1,000,000,000,000 -> 1T
     */
    private boolean decreaseValue = true;
    
    public void setDecreaseValue(boolean decrease) {
    	this.decreaseValue = decrease;
    }
    
    public String formatDecreasedValue(String value) {
    	int index = -1;
    	if ((index = value.lastIndexOf(",000,000,000,000")) != -1) {
    		return value.substring(0, index) + "T";
    	} else if ((index = value.lastIndexOf(",000,000,000")) != -1) {
    		return value.substring(0, index) + "G";
    	} else if ((index = value.lastIndexOf(",000,000")) != -1) {
    		return value.substring(0, index) + "M";
    	} else if ((index = value.lastIndexOf(",000")) != -1) {
    		return value.substring(0, index) + "K";
    	}
    	return value;
    }
    // *****************************************************************
	
	/**
	 * @return the majorTickMarkStepHint
	 */
	public int getMajorTickMarkStepHint() {
		return majorTickMarkStepHint;
	}

	/** get the scale range */ 
    public Range getRange() {
        return range;
    }

	
	/**
	 * @return the side of the tick label relative to the tick marks
	 */
	public LabelSide getTickLablesSide() {
		return tickLableSide;
	}

	/**
	 * @return the timeUnit
	 */
	public int getTimeUnit() {
		return timeUnit;
	}


	/**
	 * @return the dateEnabled
	 */
	public boolean isDateEnabled() {
		return dateEnabled;
	}

	/**
	 * @return the dirty
	 */
	public boolean isDirty() {
		return dirty;
	}



	/**
     * Gets the state indicating if log scale is enabled.
     * 
     * @return true if log scale is enabled
     */
    public boolean isLogScaleEnabled() {
        return logScaleEnabled;
    }

	
	/**
	 * @return the minorTicksVisible
	 */
	public boolean isMinorTicksVisible() {
		return minorTicksVisible;
	}

	
    /**
	 * @return the scaleLineVisible
	 */
	public boolean isScaleLineVisible() {
		return scaleLineVisible;
	}

	/**
	 * @param dateEnabled the dateEnabled to set
	 */
	public void setDateEnabled(boolean dateEnabled) {
		this.dateEnabled = dateEnabled;
        setDirty(true);
        revalidate();

	}

	/** Whenever any parameter has been changed, the scale should be marked as dirty, 
     * so all the inner parameters could be recalculated before the next paint
	 * @param dirty the dirty to set
	 */
	protected void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
     * Sets the format pattern for axis tick label. see {@link Format}
     * <p>
     * If <tt>null</tt> is set, default format will be used.
     * 
     * @param format
     *            the format
	 * @exception NullPointerException if <code>pattern</code> is null
     * @exception IllegalArgumentException if the given pattern is invalid.
     */
    public void setFormatPattern(String formatPattern) {
    	 try {
 			new DecimalFormat(formatPattern);
 		} catch (NullPointerException e) {
 			throw e;
 		} catch (IllegalArgumentException e){
 			throw e;
 		}
 		
        this.formatPattern = formatPattern;
       
        autoFormat = false;
        setDirty(true);
        revalidate();
        repaint();
    }

	/**
	 * @return the formatPattern
	 */
	public String getFormatPattern() {
		return formatPattern;
	}
	
	@Override
	public void setFont(Font f) {
		super.setFont(f);
		setDirty(true);
		revalidate();
	}

	/**
     * @param enabled true if enabling log scales
     * @throws IllegalStateException
     */
    public void setLogScale(boolean enabled) throws IllegalStateException {

        if (logScaleEnabled == enabled) {
            return;
        } 
        
        if(enabled) {
        	if(min == DEFAULT_MIN && max == DEFAULT_MAX) {
        		min = DEFAULT_LOG_SCALE_MIN;
        		max = DEFAULT_LOG_SCALE_MAX;       			
        	}
        	if(min <= 0) {
        		min = DEFAULT_LOG_SCALE_MIN;
        	}
        	if(max <= min) {
        		max = min + DEFAULT_LOG_SCALE_MAX;
        	}
        } else if(min == DEFAULT_LOG_SCALE_MIN && max == DEFAULT_LOG_SCALE_MAX) {
        	min = DEFAULT_MIN;
        	max = DEFAULT_MAX;
        }
        	
        logScaleEnabled = enabled;
        range = new Range(min, max);
        setDirty(true);
		revalidate();
		repaint();
		
    }

	/**
	 * @param majorTickMarkStepHint the majorTickMarkStepHint to set, should be less than 1000.
	 */
	public void setMajorTickMarkStepHint(int majorTickMarkStepHint) {		
		this.majorTickMarkStepHint = majorTickMarkStepHint;
		setDirty(true);
		revalidate();
		repaint();
	}

	/**
	 * @param minorTicksVisible the minorTicksVisible to set
	 */
	public void setMinorTicksVisible(boolean minorTicksVisible) {
		this.minorTicksVisible = minorTicksVisible;
	}

    /** set the scale range */
	public void setRange(final Range range) {
	    if (range == null) {
	        SWT.error(SWT.ERROR_NULL_ARGUMENT);
	        return; // to suppress warnings...
	    }
	    setRange(range.getLower(), range.getUpper());
	}

	/**Set the range with option to honor its original direction.
	 * @param t1 value 1 of the range
	 * @param t2 value 2 of the range
	 * @param honorOriginDirection if true, the start and end value of the range
	 * will set according to its original direction.
	 */
	public void setRange(double t1, double t2, boolean honorOriginDirection){
		if(honorOriginDirection){
			if(getRange().isMinBigger()){
				setRange(t1>t2? t1:t2, t1>t2?t2:t1);
			}else
				setRange(t1>t2? t2:t1, t1>t2?t1:t2);
		}else
			setRange(t1, t2);
	}
	
    /**set the scale range
     * @param lower the lower limit
     * @param upper the upper limit
     * @throws IllegalArgumentException  
     * if lower or upper is Nan of Infinite, or lower >= upper or (upper - lower) is Infinite  
     */
    public void setRange(double lower, double upper){
        if (Double.isNaN(lower) || Double.isNaN(upper) 
        		|| Double.isInfinite(lower) || Double.isInfinite(upper) || Double.isInfinite(upper-lower)) {
            throw new IllegalArgumentException("Illegal range: lower=" + lower + ", upper=" + upper);
        }
        
        //in case of lower > upper, reverse them.       
//        if(lower > upper){
//        	double temp = lower;        	
//        	lower = upper;
//        	upper = temp;
//        }        
        
       // if (min == lower && max == upper) {
       //     return;
       // }

        if (lower == upper) {
        	upper = lower +1;
        	if(Double.isInfinite(upper))
                throw new IllegalArgumentException("Illegal range: lower=" + lower + ", upper=" + upper);
        }

        if (logScaleEnabled && lower <= 0) {
        	lower = DEFAULT_LOG_SCALE_MIN;
        }

        min = lower;
        max = upper;
        
        
        //calculate the default decimal format
        if(formatPattern ==null || formatPattern == default_decimal_format) {
        	 if(Math.abs(max-min) > 0.1)
            	default_decimal_format = "############.##";
             else {
            	default_decimal_format = "##.##";
	            double mantissa = Math.abs(max-min);   
	            while (mantissa < 1) {
	                mantissa *= 10.0;
	                default_decimal_format += "#"; 
	            }
             }
        	 formatPattern = default_decimal_format;
        	 autoFormat = true;
        }
        
        if(formatPattern.equals(default_decimal_format) || 
        		formatPattern.equals(DEFAULT_ENGINEERING_FORMAT)) {
        	if((max != 0 && Math.abs(Math.log10(Math.abs(max))) >= ENGINEERING_LIMIT)
        		|| (min !=0 && Math.abs(Math.log10(Math.abs(min))) >= ENGINEERING_LIMIT))
                formatPattern = DEFAULT_ENGINEERING_FORMAT;
        	else
        		formatPattern = default_decimal_format;
        	autoFormat = true;
        } 
        range = new Range(min, max);
        setDirty(true);
        revalidate();
        repaint();
    }


	/**
	 * @param scaleLineVisible the scaleLineVisible to set
	 */
	public void setScaleLineVisible(boolean scaleLineVisible) {
		this.scaleLineVisible = scaleLineVisible;
	}

	/**
	 * @param tickLabelSide the side of the tick label relative to tick mark
	 */
	public void setTickLableSide(LabelSide tickLabelSide) {
		this.tickLableSide = tickLabelSide;
		revalidate();
	}

	/**Set the time unit for a date enabled scale. The format of the time
     * would be determined by it.
	 * @param timeUnit the timeUnit to set. It should be one of: 
	 * <tt>Calendar.MILLISECOND</tt>, <tt>Calendar.SECOND</tt>, 
	 * <tt>Calendar.MINUTE</tt>, <tt>Calendar.HOUR_OF_DAY</tt>, 
	 * <tt>Calendar.DATE</tt>, <tt>Calendar.MONTH</tt>, 
	 * <tt>Calendar.YEAR</tt>.
	 * @see Calendar
	 */
	public void setTimeUnit(int timeUnit) {
		this.timeUnit = timeUnit;
        setDirty(true);
	}

	/**
     * Updates the tick, recalculate all inner parameters
     */
    public abstract void updateTick();

	/**
	 * @param majorGridStep the majorGridStep to set
	 */
	public void setMajorGridStep(double majorGridStep) {
		this.majorGridStep = majorGridStep;
		setDirty(true);
	}

	/**
	 * @return the majorGridStep
	 */
	public double getMajorGridStep() {
		return majorGridStep;
	}

	/**
	 * @param minorTickMarkStepHint the minorTickMarkStepHint to set
	 */
	public void setMinorTickMarkStepHint(int minorTickMarkStepHint) {
		this.minorTickMarkStepHint = minorTickMarkStepHint;
	}

	/**
	 * @return the minorTickMarkStepHint
	 */
	public int getMinorTickMarkStepHint() {
		return minorTickMarkStepHint;
	}

	/**
	 * @param autoFormat the autoFormat to set
	 */
	public void setAutoFormat(boolean autoFormat) {
		this.autoFormat = autoFormat;
		if(autoFormat){
			formatPattern = null;
			setRange(getRange());
			format(0);
		}
		
	}

	/**
	 * @return the autoFormat
	 */
	public boolean isAutoFormat() {
		return autoFormat;
	}
 
}
