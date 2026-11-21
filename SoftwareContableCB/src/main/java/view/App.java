/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import javax.swing.*;

/**
 *
 * @author Usuario
 */
public class App {
    public static void main(String[] args) {
        Theme.aplicarDefaults();
        SwingUtilities.invokeLater(()->{
            ServiciosMock mock = new ServiciosMock();
            AppContext ctx = new AppContext(mock, mock, mock, mock, mock, mock);
            new MainFrame(ctx).setVisible(true);    
        });
    }
}
