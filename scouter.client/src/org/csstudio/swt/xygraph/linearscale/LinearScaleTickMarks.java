package org.csstudio.swt.xygraph.linearscale;

import java.util.ArrayList;

import org.csstudio.swt.xygraph.linearscale.AbstractScale.LabelSide;
import org.csstudio.swt.xygraph.util.SWTConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;

/**
 * Linear scale tick marks.
 * @author Xihui Chen
 */
public class LinearScaleTickMarks extends Figure {   



	/** the scale */
    private LinearScale scale;

    /** the line width */
    protected static final int LINE_WIDTH = 1;

    /** the tick length */
    public static final int MAJOR_TICK_LENGTH = 6;
    /** the tick length */
    public static final int MINOR_TICK_LENGTH = 3;

    private int minorGridStepInPixel;
    
    private int minorTicksNumber;
    
    /**
     * Constructor.
     * @param scale
     *            the scale
     */
    public LinearScaleTickMarks(LinearScale scale) {
        
        this.scale = scale;

        setForegroundColor(scale.getForegroundColor());
    }


    /**
     * Gets the associated scale.
     * 
     * @return the scale
     */
    public LinearScale getAxis() {
        return scale;
    }


   protected void paintClientArea(Graphics graphics) {
	   graphics.translate(bounds.x, bounds.y);
	   ArrayList<Integer> tickLabelPositions = scale
                .getScaleTickLabels().getTickLabelPositions();

        int width = getSize().width;
        int height = getSize().height;

        if (scale.isHorizontal()) {
            drawXTickMarks(graphics, tickLabelPositions, scale.getTickLablesSide(), width,
                    height);
        } else {
            drawYTickMarks(graphics, tickLabelPositions, scale.getTickLablesSide(), width,
                    height);
        }
   };
    
   
	/**
	 * update the parameters for minor ticks
	 */
	public void updateMinorTickParas() {
		if(scale.isDateEnabled()) {
			minorTicksNumber = 6;
			minorGridStepInPixel = (int) (scale.getScaleTickLabels().getGridStepInPixel()/6.0);
			return;
		}
			
		if(scale.getScaleTickLabels().getGridStepInPixel()/5 >= scale.getMinorTickMarkStepHint()){
			minorTicksNumber = 5;
			minorGridStepInPixel = (int) (scale.getScaleTickLabels().getGridStepInPixel()/5.0);
			return;
		}			
		if(scale.getScaleTickLabels().getGridStepInPixel()/4 >= scale.getMinorTickMarkStepHint()){
			minorTicksNumber = 4;
			minorGridStepInPixel = (int) (scale.getScaleTickLabels().getGridStepInPixel()/4.0);
			return;
		}
		
		minorTicksNumber = 2;
		minorGridStepInPixel = (int) (scale.getScaleTickLabels().getGridStepInPixel()/2.0);
		return;
	}

    /**
     * Draw the X tick marks.
     * 
     * @param tickLabelPositions
     *            the tick label positions
     * @param tickLabelSide
     *            the side of tick label relative to tick marks
     * @param width
     *            the width to draw tick marks
     * @param height
     *            the height to draw tick marks
     * @param gc
     *            the graphics context
     */
    private void drawXTickMarks(Graphics gc, ArrayList<Integer> tickLabelPositions,
            LabelSide tickLabelSide, int width, int height) {
    	
    	updateMinorTickParas();
        // draw tick marks
        gc.setLineStyle(SWTConstants.LINE_SOLID);
        
        if(scale.isLogScaleEnabled()) {
        	ArrayList<Boolean> tickLabelVisibilities = 
        		scale.getScaleTickLabels().getTickVisibilities();        	
        	for (int i = 0; i < tickLabelPositions.size(); i++) {
                int x = tickLabelPositions.get(i);
                int y = 0;
                int tickLength =0;
                if(tickLabelVisibilities.get(i))
                	tickLength = MAJOR_TICK_LENGTH;
                else
                	tickLength = MINOR_TICK_LENGTH;

                if (tickLabelSide == LabelSide.Secondary) {
                    y = height - 1 - LINE_WIDTH - tickLength;
                }
                //draw minor ticks for log scale
                if(tickLabelVisibilities.get(i) || scale.isMinorTicksVisible())
                	gc.drawLine(x, y, x, y + tickLength);
        	}
        } else {
        	for (int i = 0; i < tickLabelPositions.size(); i++) {
                int x = tickLabelPositions.get(i);
                int y = 0;
                if (tickLabelSide == LabelSide.Secondary) {
                    y = height - 1 - LINE_WIDTH - MAJOR_TICK_LENGTH;
                }
                gc.drawLine(x, y, x, y + MAJOR_TICK_LENGTH);
                //draw minor ticks for linear scale
                if(scale.isMinorTicksVisible()){
                	if(i>0) {
                		//draw the first grid step which is start from min value
                		if(i == 1 && (tickLabelPositions.get(1) - tickLabelPositions.get(0))
                				< scale.getScaleTickLabels().getGridStepInPixel()){
                			x = tickLabelPositions.get(1);
                			while((x - tickLabelPositions.get(0)) > minorGridStepInPixel + 3) {
                				x = x - minorGridStepInPixel;
                				drawXMinorTicks(gc, tickLabelSide, x, y); 
                			}
                		} //draw the last grid step which is end to max value
                		else if(i == tickLabelPositions.size()-1 && (tickLabelPositions.get(i) - tickLabelPositions.get(i-1))
                				< scale.getScaleTickLabels().getGridStepInPixel()){
                			x = tickLabelPositions.get(i-1);                			
                			while((tickLabelPositions.get(i) -x ) > minorGridStepInPixel + 3) {
                				x = x + minorGridStepInPixel;
                				drawXMinorTicks(gc, tickLabelSide, x, y); 
                			}
                		}else{ // draw regular steps
                			for(int j =0; j<minorTicksNumber; j++) {
                				x =tickLabelPositions.get(i-1) + 
                				(tickLabelPositions.get(i) - tickLabelPositions.get(i-1))*j/minorTicksNumber;
                				drawXMinorTicks(gc, tickLabelSide, x, y);
                			}  
                		}
                		              		
                	}
                }
                
            }
        }
       
            
        

        //draw scale line
        if(scale.isScaleLineVisible()) {
        	if (tickLabelSide == LabelSide.Primary) {
            gc.drawLine(scale.getMargin(), 0, width - scale.getMargin(), 0);
        } else {
            gc.drawLine(scale.getMargin(), height - 1, width - scale.getMargin(), height - 1);
        }
        }
        
    }


	private void drawXMinorTicks(Graphics gc, LabelSide tickLabelSide, int x,
			int y) {
		if(tickLabelSide == LabelSide.Primary)
			gc.drawLine(x, y, x, y + MINOR_TICK_LENGTH);
		else
			gc.drawLine(x, y + MAJOR_TICK_LENGTH - MINOR_TICK_LENGTH, 
					x, y + MAJOR_TICK_LENGTH);
	}

    /**
     * Draw the Y tick marks.
     * 
     * @param tickLabelPositions
     *            the tick label positions
     * @param tickLabelSide
     *            the side of tick label relative to tick marks
     * @param width
     *            the width to draw tick marks
     * @param height
     *            the height to draw tick marks
     * @param gc
     *            the graphics context
     */
    private void drawYTickMarks(Graphics gc, ArrayList<Integer> tickLabelPositions,
            LabelSide tickLabelSide, int width, int height) {
    	updateMinorTickParas();
        // draw tick marks
        gc.setLineStyle(SWTConstants.LINE_SOLID);
        int x = 0;
        int y = 0;
        if(scale.isLogScaleEnabled()) {
        	ArrayList<Boolean> tickLabelVisibilities = 
        		scale.getScaleTickLabels().getTickVisibilities();        	
        	for (int i = 0; i < tickLabelPositions.size(); i++) {
        		
                int tickLength =0;
                if(tickLabelVisibilities.get(i))
                	tickLength = MAJOR_TICK_LENGTH;
                else
                 	tickLength = MINOR_TICK_LENGTH;            
                
                if (tickLabelSide == LabelSide.Primary) {
                    x = width - 1 - LINE_WIDTH - tickLength;
                } else {
                    x = LINE_WIDTH;
                }
                y = height - tickLabelPositions.get(i);
                if(tickLabelVisibilities.get(i) || scale.isMinorTicksVisible())
                	gc.drawLine(x, y, x + tickLength, y);
        	}
        } else {        
            for (int i = 0; i < tickLabelPositions.size(); i++) {
                if (tickLabelSide == LabelSide.Primary) {
                    x = width - 1 - LINE_WIDTH - MAJOR_TICK_LENGTH;
                } else {
                    x = LINE_WIDTH;
                }
                y = height - tickLabelPositions.get(i);
                gc.drawLine(x, y, x + MAJOR_TICK_LENGTH, y);
                //draw minor ticks for linear scale
                if(scale.isMinorTicksVisible()){
                	if(i>0) {
                		//draw the first grid step which is start from min value
                		if(i == 1 && (tickLabelPositions.get(1) - tickLabelPositions.get(0))
                				< scale.getScaleTickLabels().getGridStepInPixel()){
                			y = tickLabelPositions.get(1);
                			while((y - tickLabelPositions.get(0)) > minorGridStepInPixel + 3) {
                				y = y - minorGridStepInPixel;
                				drawYMinorTicks(gc, tickLabelSide, x, height - y); 
                			}
                		} //draw the last grid step which is end to max value
                		else if(i == tickLabelPositions.size()-1 && (tickLabelPositions.get(i) - tickLabelPositions.get(i-1))
                				< scale.getScaleTickLabels().getGridStepInPixel()){
                			y = tickLabelPositions.get(i-1);                			
                			while((tickLabelPositions.get(i) -y ) > minorGridStepInPixel + 3) {
                				y = y + minorGridStepInPixel;
                				drawYMinorTicks(gc, tickLabelSide, x, height - y); 
                			}
                		}else{ // draw regular steps                		
	                		for(int j =0; j<minorTicksNumber; j++) {
	                			y =height - tickLabelPositions.get(i-1) -
	                				(tickLabelPositions.get(i) - tickLabelPositions.get(i-1))*j/minorTicksNumber;
	                			drawYMinorTicks(gc, tickLabelSide, x, y);
	                		}  
                		}
                	}
                }
            }
        }

        // draw scale line
        if(scale.isScaleLineVisible()) {
        	if (tickLabelSide == LabelSide.Primary) {
            gc.drawLine(width - 1, scale.getMargin(), width - 1, height - scale.getMargin());
        } else {
            gc.drawLine(0, scale.getMargin(), 0, height - scale.getMargin());
        }
        }
        
    }


	private void drawYMinorTicks(Graphics gc, LabelSide tickLabelSide, int x,
			int y) {
		//there is a misillumiation 
		int verticalMinorTickLength = MINOR_TICK_LENGTH -1;
		if(tickLabelSide == LabelSide.Primary)               				
			gc.drawLine(x + MAJOR_TICK_LENGTH - verticalMinorTickLength, y,
				x + MAJOR_TICK_LENGTH, y);
		else
			gc.drawLine(x, y,
				x + verticalMinorTickLength, y);
	}
    

}
