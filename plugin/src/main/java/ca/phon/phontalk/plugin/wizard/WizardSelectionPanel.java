package ca.phon.phontalk.plugin.wizard;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.effects.GlowPathEffect;

import ca.phon.gui.components.MultiActionButton;
import ca.phon.phontalk.plugin.action.Phon2TalkbankAction;
import ca.phon.phontalk.plugin.action.Talkbank2PhonAction;

public class WizardSelectionPanel extends JPanel {

	private static final long serialVersionUID = 7271492822403842635L;

	private MultiActionButton phon2TalkBankButton;
	
	private MultiActionButton talkBank2PhonButton;
	
	public WizardSelectionPanel() {
		super();
		
		init();
	}
	
	private void init() {
		phon2TalkBankButton = new MultiActionButton();
		phon2TalkBankButton.setTopLabelText("Phon -> TalkBank");
		phon2TalkBankButton.setBottomLabelText("<html><p>Convert Phon projects to TalkBank.</p></html>");
		phon2TalkBankButton.setBackground(Color.white);
		phon2TalkBankButton.setDefaultAction(new Phon2TalkbankAction());
		new BgPainter(phon2TalkBankButton);
		
		talkBank2PhonButton = new MultiActionButton();
		talkBank2PhonButton.setTopLabelText("TalkBank -> Phon");
		talkBank2PhonButton.setBottomLabelText("<html><p>Convert a folder of TalkBank files into a new Phon project.</b></html>");
		talkBank2PhonButton.setBackground(Color.white);
		talkBank2PhonButton.setDefaultAction(new Talkbank2PhonAction());
		new BgPainter(talkBank2PhonButton);
		
		setLayout(new VerticalLayout(5));
		add(phon2TalkBankButton);
		add(talkBank2PhonButton);
	}
	
	/**
	 * Background painter
	 */
	private class BgPainter extends MouseInputAdapter implements Painter<MultiActionButton> {

		private boolean useSelected = false;
		
		private Color origColor = null;
		
		private Color selectedColor = new Color(0, 100, 200, 100);
		
		private boolean paintPressed = false;
		
		public BgPainter(MultiActionButton btn) {
			btn.addMouseListener(this);
			btn.setBackgroundPainter(this);
			this.origColor = btn.getBackground();
		}
		
		@Override
		public void paint(Graphics2D g, MultiActionButton object, int width,
				int height) {
			// create gradient
			g.setColor((origColor != null ? origColor : Color.white));
			g.fillRect(0, 0, width, height);
			
			if(useSelected) {
//				GradientPaint gp = new GradientPaint(
//						(float)0, 0.0f, new Color(237,243, 254), (float)0.0f, (float)height, new Color(207, 213, 224), true);
//				MattePainter bgPainter = new MattePainter(gp);
//				bgPainter.paint(g, object, width, height);
//				
//				NeonBorderEffect effect  = new NeonBorderEffect();
				GlowPathEffect effect = new GlowPathEffect();
				effect.setRenderInsideShape(true);
				effect.setBrushColor(selectedColor);
				
				// get rectangle
				Rectangle2D.Double boundRect = 
					new Rectangle2D.Double(0.0f, 0.0f, width, height);
				
				effect.apply(g, boundRect, 0, 0);
			}
			
		}
		
		@Override
		public void mouseEntered(MouseEvent me) {
			useSelected = true;
			repaint();
		}
		
		@Override
		public void mouseExited(MouseEvent me) {
			useSelected = false;
			repaint();
		}
		
		@Override
		public void mousePressed(MouseEvent me) {
			if(me.getButton() == MouseEvent.BUTTON1) {
				paintPressed = true;
				repaint();
			}
		}
		
		@Override
		public void mouseReleased(MouseEvent me) {
			paintPressed = false;
			repaint();
		}
	}
}
