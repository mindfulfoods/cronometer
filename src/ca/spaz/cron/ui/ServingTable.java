/*
 * Created on 24-Nov-2005
 */
package ca.spaz.cron.ui;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import ca.spaz.cron.CRONOMETER;
import ca.spaz.cron.actions.*;
import ca.spaz.cron.datasource.FoodProxy;
import ca.spaz.cron.foods.Serving;
import ca.spaz.gui.PrettyTable;
import ca.spaz.util.ImageFactory;

public class ServingTable extends JPanel {

   private JTable table;
   private JComboBox measureBox = new JComboBox();
   private ServingTableModel model;
   private Vector listeners = new Vector();
   private Vector servingListeners = new Vector();
   private JToolBar toolBar;
   private JButton addBtn, delBtn, printBtn;
   private String title = "Untitled";
   
   public ServingTable() {
      setMinimumSize(new Dimension(400,250));
      setPreferredSize(new Dimension(400,300));
      model = new ServingTableModel(this);
      setLayout(new BorderLayout(4,4));
      add(getToolBar(), BorderLayout.NORTH);
      add(makeJScrollPane(), BorderLayout.CENTER);
   }

   public void setTitle(String str) {
      this.title = str;
   }
   public String getTitle() {
      return title;
   }
   
   public void addChangeListener(ChangeListener listener) {
      listeners.add(listener);
   }

   public void removeChangeListener(ChangeListener listener) {
      listeners.remove(listener);
   }
   
   public void addServingSelectionListener(ServingSelectionListener listener) {
      servingListeners.add(listener);
   }

   public void removeServingSelectionListener(ServingSelectionListener listener) {
      servingListeners.remove(listener);
   }
   
   protected void fireStateChangedEvent() {
      ChangeEvent e = new ChangeEvent(this);
      Iterator iter = listeners.iterator();
      while (iter.hasNext()) {
         ((ChangeListener)iter.next()).stateChanged(e);
      }
      getDeleteButton().setEnabled(table.getSelectedRow() != -1);
   }
 
   public JToolBar getToolBar() {
      if (null == toolBar) {
          toolBar = new JToolBar();
          toolBar.setRollover(true);
          toolBar.setOrientation(JToolBar.HORIZONTAL);
          toolBar.setFloatable(false);
          toolBar.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
          toolBar.add(getAddButton());
          toolBar.add(Box.createHorizontalStrut(10));
          toolBar.add(getDeleteButton());
          toolBar.add(Box.createHorizontalStrut(10));
          toolBar.add(Box.createGlue());
          toolBar.add(getPrintButton()); 
          //toolBar.add(Box.createGlue());
      }
      return toolBar;
  }
   
   
   private JButton getDeleteButton() {
      if (null == delBtn) {
          ImageIcon icon = new ImageIcon(ImageFactory.getInstance().loadImage("/img/trash.gif"));
          delBtn = new JButton("Delete Serving", icon);
          delBtn.setEnabled(false);
          delBtn.setToolTipText("Delete the selected serving.");
          delBtn.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                 deleteSelectedServings();
              }
          });
          CRONOMETER.fixButton(delBtn);
      }
      return delBtn;
  }
   
   private JButton getAddButton() {
      if (null == addBtn) {
          ImageIcon icon = new ImageIcon(ImageFactory.getInstance().loadImage("/img/add.gif"));
          addBtn = new JButton("Add Serving", icon);
          addBtn.setToolTipText("Add a new serving.");
          addBtn.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                 doAddServing();
              }
          });
          CRONOMETER.fixButton(addBtn);
      }
      return addBtn;
  }

  
   
   public void doAddServing() {
     SearchDialog sd = new SearchDialog(JOptionPane.getFrameForComponent(this));
     sd.display(true);
      
      Serving s = sd.getSelectedServing();
      if (s != null) { 
         fireServingChosen(s);
      }      
   }
   
   private JComponent makeJScrollPane() {
      JScrollPane jsp = new JScrollPane(getTable());
      jsp.setPreferredSize(new Dimension(400, 250));
      jsp.getViewport().setBackground(Color.WHITE);
      jsp.setBorder(BorderFactory.createEtchedBorder());
      return jsp;
   }

   protected JTable getTable() {
      if (null == table) {
         table = new PrettyTable()  {
            public String getToolTipText(MouseEvent e) {
               return model.getToolTipText(
                     rowAtPoint(e.getPoint()),
                     columnAtPoint(e.getPoint()));
            }
         };
         table.setModel(model);
         table.setColumnSelectionAllowed(false);
         table.getSelectionModel().setSelectionMode(
               ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
         table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
         table.getTableHeader().setReorderingAllowed(false);

         table.getColumnModel().getColumn(ServingTableModel.AMOUNT_COL).setMaxWidth(60);
         table.getColumnModel().getColumn(ServingTableModel.MEASURE_COL).setMaxWidth(100);
         table.getColumnModel().getColumn(ServingTableModel.CALORIES_COL).setMaxWidth(60);

         table.getColumnModel().getColumn(ServingTableModel.MEASURE_COL).setCellEditor(
                     new DefaultCellEditor(measureBox));
                   
         // right align last column
         TableColumnModel tcm = table.getColumnModel();
         DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
         renderer.setHorizontalAlignment(SwingConstants.RIGHT);
         tcm.getColumn(ServingTableModel.AMOUNT_COL).setCellRenderer(renderer);
         tcm.getColumn(ServingTableModel.CALORIES_COL).setCellRenderer(renderer);

         table.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
               public void valueChanged(ListSelectionEvent e) {
                 /* if (e.getValueIsAdjusting()) return;*/
                  ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                  if (!lsm.isSelectionEmpty()) {
                     int sel = table.getSelectedRow();
                     if (sel != -1) {
                        fireServingSelected(model.getServing(sel));
                     }
                  }
                  fireStateChangedEvent();
               }
            });
         addTableClickListener();
  
         
         table.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               deleteSelectedServings();
            }
         }, "Clear", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false), JComponent.WHEN_FOCUSED);
         
         table.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               deleteSelectedServings();
            }
         }, "Clear", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, false), JComponent.WHEN_FOCUSED);
         
         table.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               doCopy();
            }
         }, "Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false), JComponent.WHEN_FOCUSED);
         
         table.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               doPaste();
            }
         }, "Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false), JComponent.WHEN_FOCUSED);
         
         table.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               doCut();
            }
         }, "Cut", KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK, false), JComponent.WHEN_FOCUSED);
       
      }
      return table;
   } 
    

   public void doClear() {
      deleteSelectedServings();
   }

   public void doCut() {
      doCopy();
      doClear();
   }
   
   public void doCopy() {
      copySelectedServings();
   }   

   /**
    * Add a list of servings to the daily listing
    * Ugly because this table and model listens to the parent, which is 
    * backwards from normal patterns...
    * @param list
    */
   public void addServings(Serving[] list) {
      if (CRONOMETER.getInstance().getDailySummary().isOkToAddServings()) {
         for (int i=0; i<list.length; i++) {
            CRONOMETER.getInstance().getDailySummary().addServing(new Serving(list[i]));
         }
      }
   }
   
   public void doPaste() {
      Transferable clipboardContent = CRONOMETER.getClipboard().getContents(this);
      if (clipboardContent != null) {
         if (clipboardContent.isDataFlavorSupported(ServingSelection.servingFlavor)) {         
            try {               
               Serving[] list = (Serving[])clipboardContent.getTransferData(ServingSelection.servingFlavor);
               if (list.length > 0) {
                  //int sel = table.getSelectedRow();
                  //doClear();
                  addServings(list); 
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }
   
   public void deleteSelectedServings() {
      List sel = getSelectedServings();
      // TODO: "Are you sure"?
      if (sel.size() > 0) {
         Iterator iter = sel.iterator();
         while (iter.hasNext()) {
            Serving s = (Serving)iter.next();
            model.delete(s);
            s.delete(); // TODO: is this safe for recipes?
         }
         deselect();
         fireStateChangedEvent();
      }
   }
   
   
   public void copySelectedServings() {    
      CRONOMETER.getClipboard().setContents (new ServingSelection(this), CRONOMETER.getInstance());
      // enable paste menu...
   }
   
   /**
    * Installs a click listener to handle contextual 
    * pop-up menus on row selections
    */
   private void addTableClickListener() {     
      table.addMouseListener(new MouseAdapter() {
         int last = -1;
         public void mouseClicked(MouseEvent e) {
            int index = table.rowAtPoint(e.getPoint());
            
            if (e.getButton() == MouseEvent.BUTTON3 || e.isControlDown()) {
               if (index >= 0) {                
                  if (!table.isRowSelected(index)) {
                     table.getSelectionModel().setSelectionInterval(index,index);
                  }
                  handleMouseClick(e);               
               }
            } else {
               if (e.getClickCount() == 2) {
                  Serving s = model.getServing(index);
                  fireServingDoubleClicked(s);
               } else {
                  if (last == index) {
                     table.getSelectionModel().clearSelection();
                     last = -1;
                  } else {
                     last = index;
                  }
               }
            }
         }        
      });          
   }


   /**
    * Set the comboBox table cell editor to the currently
    * selected measure list
    * @param serving the selected serving
    */
   private void setMeasureBox(Serving s) {
      if (s != null) {
         measureBox.removeAllItems();
         List measures = s.getFood().getMeasures();
         for (int i=0; i<measures.size(); i++) {
            measureBox.addItem(measures.get(i));
         }
         measureBox.setSelectedItem(s.getMeasure());
      }
   }
  
   private void fireServingSelected(Serving s) {
      if (s == null) return;
      setMeasureBox(s);
      getDeleteButton().setEnabled(!getSelectedServings().isEmpty());
      Iterator iter = servingListeners.iterator();
      while (iter.hasNext()) {
         ((ServingSelectionListener)iter.next()).servingSelected(s);
      }
      getTable().requestFocus();      
   }
   
   private void fireServingDoubleClicked(Serving s) {
      if (s == null) return;
      Iterator iter = servingListeners.iterator();
      while (iter.hasNext()) {
         ((ServingSelectionListener)iter.next()).servingDoubleClicked(s);
      }
   }
   
   private void fireServingChosen(Serving s) {
      if (s == null) return;
      Iterator iter = servingListeners.iterator();
      while (iter.hasNext()) {
         ((ServingSelectionListener)iter.next()).servingChosen(s);
      }
   }
   
   public void deselect() {
      getTable().getSelectionModel().clearSelection();
   }
   
   public List getSelectedServings() {
      List servings = new ArrayList();
      if (table.getSelectedRow() != -1) {
         int[] rows = table.getSelectedRows();
         for (int i=0; i<rows.length; i++) {
            servings.add(model.getServing(rows[i]));
         }
      }
      return servings;
   }

   public void setServings(List consumed) {
      model.setServings(consumed); 
      fireStateChangedEvent();
   }

   public List getServings() {
      return model.getServings();
   }
   
   public void addServing(Serving s) {
      model.addServing(s);
      fireStateChangedEvent();
   }
   
   private void handleMouseClick(MouseEvent e) {
      JPopupMenu menu = new JPopupMenu();
      if (getSelectedServings().size() == 1) { // single item selected
         FoodProxy f = ((Serving)getSelectedServings().get(0)).getFoodProxy();
         menu.add(new EditFoodAction(f, this));
         menu.add(new ExportFoodAction(f, this));
      } else { // multiple items selected
         menu.add(new CreateRecipeAction(getSelectedServings(), this));         
      }
      // actions that apply to both single and multiple selections:
      menu.addSeparator();
      menu.add(new DeleteServingsAction(this));
      
      if (menu.getComponents().length > 0) {
         menu.show(table, e.getX(), e.getY());
      }      
   }
  

   private JButton getPrintButton() {
      if (null == printBtn) {
         ImageIcon icon = new ImageIcon(ImageFactory.getInstance().loadImage(
               "/img/print.gif"));
         printBtn = new JButton(icon);
         printBtn.setToolTipText("Print the food listing.");
         printBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               doPrint();
            }
         });
         CRONOMETER.fixButton(printBtn);
      }
      return printBtn;
   }
   
   /**
    * Does a very simple print-out of the recipe.
    */
   public void doPrint() {
      try {         
         MessageFormat headerFormat = new MessageFormat(getTitle());
         MessageFormat footerFormat = new MessageFormat("- {0} -");
         getTable().print(JTable.PrintMode.FIT_WIDTH, headerFormat, footerFormat);          
      } catch (PrinterException e) {
         e.printStackTrace();
         JOptionPane.showMessageDialog(this, e.getMessage());
      }
   }     
   
  
}
