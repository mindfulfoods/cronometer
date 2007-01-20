/*
 * Created on Apr 2, 2005 by davidson
 */
package ca.spaz.cron.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ca.spaz.cron.CRONOMETER;
import ca.spaz.cron.datasource.Datasources;
import ca.spaz.cron.foods.Serving;
import ca.spaz.cron.summary.NutritionSummaryPanel;
import ca.spaz.gui.TranslucentToolBar;
import ca.spaz.util.ImageFactory;
import ca.spaz.util.ToolBox;

/**
 * Shows all data for a particular date
 * 
 * @todo: Calendar widget to skip to ANY date
 * 
 * @author davidson
 */
public class DailySummary extends JPanel { 

   private static final long ONE_DAY = 1000 * 60 * 60 * 24;

   private BiomarkerPanel bioMarkerPanel;

   private Date curDate = new Date(System.currentTimeMillis());
 
   private ServingTable servingTable;
   private JTabbedPane dailyTracker;

   private DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);

   private JSplitPane dietPanel;
 
   private JButton nextButton; 
   private JButton prevButton;
   private JButton titleLabel;
   private JButton copyPrevDayButton;
   private JButton todayButton;
   private JButton prefsButton;
   private JButton dbButton;
   
   private TranslucentToolBar toolBar;
   private NutritionSummaryPanel totals;
   boolean asked = false; 
   
   public DailySummary() { 
      setPreferredSize(new Dimension(580,640));
      initialize();
      setDate(curDate);
      notifyObservers();      
   }

   public void addServing(Serving c) {
      if (isOkToAddServings()) {
         Serving copy = new Serving(c);
         copy.setDate(curDate);
         Datasources.getFoodHistory().addServing(copy);
         notifyObservers(); 
      }
   }
   
   public boolean isOkToAddServings() {
      Date now = new Date(System.currentTimeMillis());
      if (!ToolBox.isSameDay(curDate, now) && !asked) {        
         int choice = JOptionPane.showConfirmDialog(this, 
               "You are adding a food to a date in the past or future.\nAre you sure you want to do this?",
               "Add food?", JOptionPane.YES_NO_OPTION);
         if (choice != JOptionPane.YES_OPTION) {
            return false;
         }
      }
      asked = true;
      return true;
   } 
   
   /**
    * Prompt the user for a specific calendar date and set the panel to that
    * current date.
    */
   public void chooseDate() {
      pickDate();
   }


   private JPanel getBioMarkersPanel() {
      if (null == bioMarkerPanel) {
         bioMarkerPanel = new BiomarkerPanel();
      }
      return bioMarkerPanel;
   }

   private JTabbedPane getDailyTrackerPanel() {
      if (null == dailyTracker) {
         dailyTracker = new JTabbedPane();
         dailyTracker.addTab("Diet", new ImageIcon(ImageFactory.getInstance().loadImage("/img/apple-16x16.png")), getDietPanel()); 
         dailyTracker.addTab("Biomarkers", new ImageIcon(ImageFactory.getInstance().loadImage("/img/graph.gif")), getBioMarkersPanel());
         dailyTracker.addTab("Exercise", new ImageIcon(ImageFactory.getInstance().loadImage("/img/lockedstate.gif")), new JPanel());
         dailyTracker.addTab("Notes", new ImageIcon(ImageFactory.getInstance().loadImage("/img/toc_open.gif")), new JPanel());
         
      }
      return dailyTracker;
   }

   private JSplitPane getDietPanel() {
      if (null == dietPanel) {
         dietPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
               getServingTable(), getNutritionSummaryPanel());
         dietPanel.setDividerLocation(300);
         dietPanel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3)); 
      }
      return dietPanel;
   }
   
   public ServingTable getServingTable() {
      if (null == servingTable) {
         servingTable = new ServingTable();
         servingTable.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
               List servings = servingTable.getSelectedServings();
               if (servings.size() == 0) {
                  servings = servingTable.getServings();
               }
               totals.setServings(servings);               
            }           
         });               
      }
      return servingTable;
   }


   
   private JButton getPrefsButton() {
      if (null == prefsButton) {
         ImageIcon icon = new ImageIcon(ImageFactory.getInstance().loadImage("/img/task.gif"));
         prefsButton = new JButton(icon);         
         CRONOMETER.fixButton(prefsButton);    
         prefsButton.setToolTipText("Edit Nutritional Targets");
         prefsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               CRONOMETER.getInstance().doEditUserSettings();
            }
         });
      }
      return prefsButton;
   }
     

   private JButton getNextButton() {
      if (null == nextButton) {
         nextButton = new JButton(new ImageIcon(ImageFactory.getInstance().loadImage("/img/forth.gif")));
         nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               setDate(new Date(curDate.getTime() + ONE_DAY));
            }  
         }); 
         nextButton.setFocusable(false); 
         CRONOMETER.fixButton(nextButton); 
         nextButton.setToolTipText("Next Day");          
      }
      return nextButton;
   }
 

   private JButton getPreviousButton() {
      if (null == prevButton) {
         prevButton = new JButton(new ImageIcon(ImageFactory.getInstance().loadImage("/img/back.gif")));
         prevButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               setDate(new Date(curDate.getTime() - ONE_DAY));
            }  
         }); 
         CRONOMETER.fixButton(prevButton);
         prevButton.setToolTipText("Previous Day");         
         prevButton.setFocusable(false); 
      }
      return prevButton;
   }

   private JButton getCopyPreviousDayButton() {
      if (null == copyPrevDayButton) {
         copyPrevDayButton = new JButton(new ImageIcon(ImageFactory.getInstance().loadImage("/img/copy.gif")));
         copyPrevDayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               copyPreviousDay();
            }
         });
         CRONOMETER.fixButton(copyPrevDayButton);
         copyPrevDayButton.setToolTipText("Copy Previous Day");
         copyPrevDayButton.setFocusable(false);
      }
      return copyPrevDayButton;
   }   
   
//   private JButton getDatabaseButton() {
//      if (null == dbButton) {
//         dbButton = new JButton(new ImageIcon(ImageFactory.getInstance().loadImage("/img/apple-16x16.png")));
//         dbButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//               CRONOMETER.getInstance().doBrowseFoodDatabase();
//            }
//         });
//         CRONOMETER.fixButton(dbButton);
//         dbButton.setToolTipText("Browse Food Database");
//         dbButton.setFocusable(false);
//      }
//      return dbButton;
//   }   
   
   private JButton getTodayButton() {
      if (null == todayButton) {
         todayButton = new JButton(new ImageIcon(ImageFactory.getInstance().loadImage("/img/trace.gif")));
         todayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               goToToday();
            }
         });
         CRONOMETER.fixButton(todayButton);
         todayButton.setToolTipText("Go To Today");
         todayButton.setFocusable(false);
      }
      return todayButton;
   }   

   /**
    * Set the current date to today
    */
   public void goToToday() {
      setDate(new Date(System.currentTimeMillis()));
   }
   
   /**
    * Copies the foods from the previous day into this day.
    */
   private void copyPreviousDay() {
      if (isOkToAddServings()) {
   	   Date previousDay = new Date(curDate.getTime() - ONE_DAY);
   	   Datasources.getFoodHistory().copyConsumedOn(previousDay, curDate);
   	   notifyObservers();
      }
   }

   /**
    * @return the title label for this component
    */
   private JButton getTitle() {
      if (null == titleLabel) {
         titleLabel = new JButton(df.format(curDate));
         titleLabel.setFont(new Font("Application", Font.BOLD, 16)); 
         titleLabel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               chooseDate();
            }
         });
         CRONOMETER.fixButton(titleLabel); 
      }
      return titleLabel;
   }
   

   private JComponent getToolbar() {
      if (null == toolBar) {
         toolBar = new TranslucentToolBar(0.25);
         toolBar.setBackground(Color.BLACK); 
         toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
         toolBar.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
         //toolBar.add(getDatabaseButton());         
         //toolBar.add(Box.createHorizontalStrut(5));
         toolBar.add(getTodayButton());         
         toolBar.add(Box.createHorizontalGlue());
         toolBar.add(getPreviousButton());
         toolBar.add(Box.createHorizontalStrut(5));
         toolBar.add(getTitle());
         toolBar.add(Box.createHorizontalStrut(5));
         toolBar.add(getNextButton());
         toolBar.add(Box.createHorizontalGlue());
         toolBar.add(getCopyPreviousDayButton());  
         toolBar.add(Box.createHorizontalStrut(5));
         toolBar.add(getPrefsButton()); 
      }
      return toolBar;
   }
 
   public NutritionSummaryPanel getNutritionSummaryPanel() {
      if (null == totals) {
         totals = new NutritionSummaryPanel();
      }
      return totals;
   }

   private void initialize() {
      setLayout(new BorderLayout(4, 4));
      setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 3));
      add(getToolbar(), BorderLayout.NORTH);
      add(getDailyTrackerPanel(), BorderLayout.CENTER);
   }

   public void notifyObservers() {
     
      List consumed = Datasources.getFoodHistory().getConsumedOn(curDate);
      getServingTable().setServings(consumed);
   }

   public void setDate(Date d) {
      curDate = d;
      bioMarkerPanel.setDate(d);
      getTitle().setText(df.format(curDate));
      getServingTable().setTitle(df.format(curDate));
      asked = false;
      getTodayButton().setEnabled(!ToolBox.isSameDay(d, new Date(System.currentTimeMillis())));
      notifyObservers();
   }

   public void pickDate() {
      Date d = DateChooser.pickDate(this, curDate);
      if (d != null) {
         setDate(d);
      }
   }

 
     
}
