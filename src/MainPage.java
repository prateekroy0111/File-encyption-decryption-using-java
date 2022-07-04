import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hp
 */
class MyCustomFilter extends javax.swing.filechooser.FileFilter {
   @Override
   public boolean accept(File file) {
      // Allow only directories, or files with ".txt" extension
      return file.isDirectory() || file.getAbsolutePath().endsWith(".txt");
   }
   @Override
   public String getDescription() {
      // This description will be displayed in the dialog,
      // hard-coded = ugly, should be done via I18N
      return "Text documents (*.txt)";
   }
}



public class MainPage extends javax.swing.JFrame {

   /**
    * Creates new form MainPage
    */
   
   Connection conn;
   ResultSet rs;
   PreparedStatement pst,pst2;
   Statement stmt;
   String user_id, file_name, enc_name;
   byte[] cipherText;
   
     
   public MainPage() {
      super("Encryption - Decryption");
      initComponents();
   }
   
   public MainPage(String user_id) {
      super("Encryption - Decryption");
      initComponents();
      conn=Db_Connect.ConnectDb();
      this.user_id = user_id;
   }
   
    boolean validateMethod(){                
        if (jTextArea1.getText() == null || jTextArea1.getText().trim().isEmpty()){
            JOptionPane.showMessageDialog(null, "Please choose the file");
            jTextArea1.requestFocus();
            return false;
        }
        else if (enc_name == null || enc_name.trim().isEmpty()){
            JOptionPane.showMessageDialog(null, "Choose Encryption/Decryption Strength");
            return false;
        }
        else {
            return true;
        }
    }
    
    boolean validateMethod2(){                
        if (jTextArea1.getText() == null || jTextArea1.getText().trim().isEmpty()){
            JOptionPane.showMessageDialog(null, "Please choose the file");
            jTextArea1.requestFocus();
            return false;
        }
        else {
            return true;
        }
    }
    
    String getCurrentTime()
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now); 
    }
   
    void encrypt_caesar_cipher()
    {
        String plainText = jTextArea1.getText();
        
        String current_time = getCurrentTime();        
        
        String new_filename = "enc_" + current_time.replaceAll("[^a-zA-Z0-9]+", "") + "_" + file_name;
        
        Encrypt_Decrypt_2 obj = new Encrypt_Decrypt_2();
        int key = obj.generateRandomNumber();
        String encryptedString = obj.encryption(plainText, key);
        String secretKey = String.valueOf(key);
        
        //Storing details in DB
        String sql1="insert into file_info (file_name,enc_method,enc_date_time,user_id) values (?,?,?,?)";
        try{
            pst=conn.prepareStatement(sql1);
            pst.setString(1, new_filename);
            pst.setString(2, enc_name);
            pst.setString(3, current_time);
            pst.setString(4, user_id);
            pst.execute();
        }catch(Exception ex){
            JOptionPane.showMessageDialog(null, ex);
        }

        String sql2="insert into caesar_cipher_details (file_name,secretKey) values (?,?)";
        try{
            pst2=conn.prepareStatement(sql2);
            pst2.setString(1, new_filename);
            pst2.setString(2, secretKey);
            pst2.execute();
        }catch(Exception ex){
            JOptionPane.showMessageDialog(null, ex);
        }
        
        jTextArea1.setText(encryptedString);
            
        try {
            //writing Encrypted text to new file
            FileWriter myWriter = new FileWriter(new_filename);
            myWriter.write(encryptedString);
            myWriter.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        JOptionPane.showMessageDialog(null, "Text Encrypted Successfully!!!");
   }
    
   void decrypt_caesar_cipher()
   {
       try{
       
        String current_time = getCurrentTime();
        
        String new_filename = "dec_" + current_time.replaceAll("[^a-zA-Z0-9]+", "") + ".txt";
        
        String key = "";
        String encString = jTextArea1.getText();
        StringBuilder encText = new StringBuilder(encString);

        String sql="select * from caesar_cipher_details where file_name=?";
        try{
            pst=conn.prepareStatement(sql);
            pst.setString(1, file_name);
            rs=pst.executeQuery();
            if(rs.next()){
                key=rs.getString("secretKey");
            }
            else{
                JOptionPane.showMessageDialog(null, "File Details don\'t match!!!");
            }
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
        }
        
        int secretKey = Integer.parseInt(key);
        Encrypt_Decrypt_2 obj = new Encrypt_Decrypt_2();
        String decryptedString = obj.decryption(encText, secretKey);
        
        jTextArea1.setText(decryptedString);
        
        //writing Encrypted text to new file
        FileWriter myWriter = new FileWriter(new_filename);
        myWriter.write(decryptedString);
        myWriter.close();
        
        JOptionPane.showMessageDialog(null, "Text Decrypted Successfully!!!");
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
        }
   }
   
   void encrypt_RSA()
   {
        BigInteger p,q,n,phi,d,e,cipherMessage,encrypted;
        
        String plainText = jTextArea1.getText();
        
        String current_time = getCurrentTime();
        
        String new_filename = "enc_" + current_time.replaceAll("[^a-zA-Z0-9]+", "") + "_" + file_name;
        
        //for RSA
        Encrypt_Decrypt_RSA rsa = new Encrypt_Decrypt_RSA();
                
        p = rsa.largePrime(512);
        q = rsa.largePrime(512);
        n = rsa.n(p, q);
        phi = rsa.getPhi(p, q);
        e = rsa.genE(phi);
        d = rsa.extEuclid(e, phi)[1];
        
        // Convert string to numbers using a cipher
        cipherMessage = rsa.stringCipher(plainText);
        // Encrypt the ciphered message
        encrypted = rsa.encrypt(cipherMessage, e, n);
        
        try {
            //Storing details in DB
            String sql1="insert into file_info (file_name,enc_method,enc_date_time,user_id) values (?,?,?,?)";
            try{
                pst=conn.prepareStatement(sql1);
                pst.setString(1, new_filename);
                pst.setString(2, enc_name);
                pst.setString(3, current_time);
                pst.setString(4, user_id);
                pst.execute();
            }catch(Exception ex){
                JOptionPane.showMessageDialog(null, ex);
            }
            
            String sql2="insert into rsa_details (file_name,e,n,d) values (?,?,?,?)";
            try{
                pst2=conn.prepareStatement(sql2);
                pst2.setString(1, new_filename);
                pst2.setString(2, e.toString());
                pst2.setString(3, n.toString());
                pst2.setString(4, d.toString());
                pst2.execute();
            }catch(Exception ex){
                JOptionPane.showMessageDialog(null, ex);
            }
            
            jTextArea1.setText(encrypted.toString());
            
            try {
                //writing Encrypted text to new file
                FileWriter myWriter = new FileWriter(new_filename);
                myWriter.write(encrypted.toString());
                myWriter.close();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex);
            }
            
            JOptionPane.showMessageDialog(null, "Text Encrypted Successfully!!!");
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex);
        }
   }
   
   void decrypt_RSA()
   {
       BigInteger d,n,encrypted,decrypted;
       String dd="",nn="";
       try{
        
        encrypted = new BigInteger(jTextArea1.getText());
        
        String current_time = getCurrentTime();
                
        String sql="select * from rsa_details where file_name=?";
        try{
            pst=conn.prepareStatement(sql);
            pst.setString(1, file_name);
            rs=pst.executeQuery();
            if(rs.next()){
                dd=rs.getString("d");
                nn=rs.getString("n");
            }
            else{
                JOptionPane.showMessageDialog(null, "File Details don\'t match!!!");
            }
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
        }
        
        Encrypt_Decrypt_RSA rsa = new Encrypt_Decrypt_RSA();
        d = new BigInteger(dd);
        n = new BigInteger(nn);
        
        decrypted = rsa.decrypt(encrypted, d, n);
        String decryptedText = rsa.cipherToString(decrypted);
        
        jTextArea1.setText(decryptedText);
        
        //writing Encrypted text to new file
        String new_filename = "dec_" + current_time.replaceAll("[^a-zA-Z0-9]+", "") + ".txt";
        FileWriter myWriter = new FileWriter(new_filename);
        myWriter.write(decryptedText);
        myWriter.close();

        JOptionPane.showMessageDialog(null, "Text Decrypted Successfully!!!");
        
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
        }
   }
   
   /**
    * This method is called from within the constructor to initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is always
    * regenerated by the Form Editor.
    */
   @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        encrypt_button = new javax.swing.JButton();
        decrypt_button = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        open = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exit = new javax.swing.JMenuItem();

        fileChooser.setDialogTitle("This is my open dialog");
        fileChooser.setFileFilter(new MyCustomFilter());

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLocation(new java.awt.Point(375, 150));

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        encrypt_button.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        encrypt_button.setText("Encrypt");
        encrypt_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                encrypt_buttonActionPerformed(evt);
            }
        });

        decrypt_button.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        decrypt_button.setText("Decrypt");
        decrypt_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decrypt_buttonActionPerformed(evt);
            }
        });

        jButton3.setText("Logout");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setText("Strong");
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setText("Weak");
        jRadioButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton3ActionPerformed(evt);
            }
        });

        jButton1.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        jButton1.setText("OK");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Back");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Encryption Strength");

        jMenu1.setText("File");
        jMenu1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        open.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        open.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        open.setText("Open");
        open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openActionPerformed(evt);
            }
        });
        jMenu1.add(open);
        jMenu1.add(jSeparator1);

        exit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        exit.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        exit.setText("Exit");
        exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitActionPerformed(evt);
            }
        });
        jMenu1.add(exit);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(encrypt_button)
                            .addComponent(decrypt_button)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jRadioButton3)
                                    .addComponent(jRadioButton1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButton1))
                            .addComponent(jLabel1)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jButton2)
                        .addGap(18, 18, 18)
                        .addComponent(jButton3)))
                .addContainerGap(13, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jRadioButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jRadioButton3)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(encrypt_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(decrypt_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton2)
                            .addComponent(jButton3))))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

   private void openActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openActionPerformed
      // TODO add your handling code here:
      int returnVal = fileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try {
            
            //read file contents
            Scanner scanner = new Scanner( new File(file.getAbsolutePath()), StandardCharsets.UTF_8.name());
            String plainText = scanner.useDelimiter("\\A").next();
            scanner.close();

            //show file contents
            jTextArea1.setText(plainText);

            //store the file nsme
            Path path = Paths.get(file.getAbsolutePath()); 
            Path fileName = path.getFileName(); 
            file_name = fileName.toString();

        } catch (IOException ex) {
           JOptionPane.showMessageDialog(null, "Problem accessing file. "+file.getAbsolutePath());
        }
      } else { 
        JOptionPane.showMessageDialog(null,"File access cancelled by user.");
      }
   }//GEN-LAST:event_openActionPerformed

   private void exitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitActionPerformed
      // TODO add your handling code here:
      System.exit(0);
   }//GEN-LAST:event_exitActionPerformed

   private void decrypt_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decrypt_buttonActionPerformed
        // TODO add your handling code here:
        if(validateMethod2()==true)
        {
            String sql="select * from file_info where file_name=?";
            try{
                pst=conn.prepareStatement(sql);
                pst.setString(1, file_name);
                rs=pst.executeQuery();
                if(rs.next()){
                    enc_name=rs.getString("enc_method");
                    
                    if ("RSA".equals(enc_name))
                    {
                        decrypt_RSA();
                    }
                    if ("caesar_cipher".equals(enc_name))
                    {
                        decrypt_caesar_cipher();
                    }
                }
                else{
                    JOptionPane.showMessageDialog(null, "File Details don\'t match!!!");
                }
            }catch(Exception e){
                JOptionPane.showMessageDialog(null, e);
            }
        }
   }//GEN-LAST:event_decrypt_buttonActionPerformed

   private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
      // TODO add your handling code here:
      setVisible(false);
      Login ob=new Login();
      ob.setVisible(true);
   }//GEN-LAST:event_jButton3ActionPerformed

    private void encrypt_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encrypt_buttonActionPerformed
        // TODO add your handling code here:
        if(validateMethod()==true)
        {
            if ("RSA".equals(enc_name))
            {
                encrypt_RSA();
            }
            if ("caesar_cipher".equals(enc_name))
            {            
                encrypt_caesar_cipher();
            }
        }
    }//GEN-LAST:event_encrypt_buttonActionPerformed

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        if (jRadioButton1.isSelected()){
            enc_name = "RSA";
        }
        if (jRadioButton3.isSelected()){
            enc_name = "caesar_cipher";
        }
        JOptionPane.showMessageDialog(null,"Algorithm to be used = "+enc_name);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        setVisible(false);
        HomePage ob=new HomePage(user_id);
        ob.setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton3ActionPerformed

   /**
    * @param args the command line arguments
    */
   public static void main(String args[]) {
      /* Set the Nimbus look and feel */
      //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
      /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
       * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
       */
      try {
         for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
               javax.swing.UIManager.setLookAndFeel(info.getClassName());
               break;
            }
         }
      } catch (ClassNotFoundException ex) {
         java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (InstantiationException ex) {
         java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex) {
         java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (javax.swing.UnsupportedLookAndFeelException ex) {
         java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      }
      //</editor-fold>
   
      /* Create and display the form */
      java.awt.EventQueue.invokeLater(
         new Runnable() {
            public void run() {
               new MainPage().setVisible(true);
            }
         });
   }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton decrypt_button;
    private javax.swing.JButton encrypt_button;
    private javax.swing.JMenuItem exit;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JMenuItem open;
    // End of variables declaration//GEN-END:variables
}
