package com.test.java.com.sergio.facelectronica;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Campo de fecha facil de usar: se puede escribir la fecha (dd/MM/aaaa) o elegirla
 * en un calendario mensual que se abre con el boton de la derecha.
 */
public class SelectorFecha extends JPanel {
    private static final DateTimeFormatter FORMATO=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Locale ESPANOL=new Locale("es", "CO");
    private static final Color COLOR_ERROR=new Color(255, 225, 225);

    private final JTextField texto=new JTextField(10);
    private final JButton botonCalendario=new JButton("▼");
    private final JPopupMenu popup=new JPopupMenu();
    private final Color fondoNormal;
    private YearMonth mesVisible;

    public SelectorFecha(LocalDate inicial){
        super(new BorderLayout(4, 0));
        fondoNormal=texto.getBackground();
        texto.setToolTipText("Escriba la fecha como dd/mm/aaaa o use el calendario");
        botonCalendario.setToolTipText("Abrir calendario");
        botonCalendario.setMargin(new Insets(1, 6, 1, 6));
        botonCalendario.setFocusable(false);
        botonCalendario.addActionListener(e -> MostrarCalendario());
        texto.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { MarcarValidez(); }
            public void removeUpdate(DocumentEvent e) { MarcarValidez(); }
            public void changedUpdate(DocumentEvent e) { MarcarValidez(); }
        });
        add(texto, BorderLayout.CENTER);
        add(botonCalendario, BorderLayout.EAST);
        setFecha(inicial);
    }

    /**
     * Fecha digitada o seleccionada, o null si el texto no es una fecha valida.
     */
    public LocalDate getFecha(){
        try {
            return LocalDate.parse(texto.getText().trim(), FORMATO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    public void setFecha(LocalDate fecha){
        texto.setText(fecha==null ? "" : fecha.format(FORMATO));
    }
    @Override
    public void setEnabled(boolean habilitado){
        super.setEnabled(habilitado);
        texto.setEnabled(habilitado);
        botonCalendario.setEnabled(habilitado);
    }
    @Override
    public void requestFocus(){
        texto.requestFocus();
    }

    private void MarcarValidez(){
        texto.setBackground(getFecha()==null && !texto.getText().trim().isEmpty() ? COLOR_ERROR : fondoNormal);
    }

    private void MostrarCalendario(){
        LocalDate actual=getFecha();
        mesVisible=YearMonth.from(actual!=null ? actual : LocalDate.now());
        ConstruirCalendario();
        popup.show(this, 0, getHeight());
    }

    private void ConstruirCalendario(){
        popup.removeAll();
        JPanel calendario=new JPanel(new BorderLayout(0, 4));
        calendario.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel encabezado=new JPanel(new BorderLayout());
        JPanel izquierda=new JPanel(new GridLayout(1, 2, 2, 0));
        izquierda.add(BotonNavegar("«", "Ano anterior", -12));
        izquierda.add(BotonNavegar("‹", "Mes anterior", -1));
        JPanel derecha=new JPanel(new GridLayout(1, 2, 2, 0));
        derecha.add(BotonNavegar("›", "Mes siguiente", 1));
        derecha.add(BotonNavegar("»", "Ano siguiente", 12));
        String nombreMes=mesVisible.getMonth().getDisplayName(TextStyle.FULL, ESPANOL);
        JLabel titulo=new JLabel(nombreMes.substring(0, 1).toUpperCase()+nombreMes.substring(1)+" "+mesVisible.getYear(), SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD));
        encabezado.add(izquierda, BorderLayout.WEST);
        encabezado.add(titulo, BorderLayout.CENTER);
        encabezado.add(derecha, BorderLayout.EAST);
        calendario.add(encabezado, BorderLayout.NORTH);

        JPanel dias=new JPanel(new GridLayout(0, 7, 2, 2));
        for (DayOfWeek dia : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY}) {
            JLabel nombre=new JLabel(dia.getDisplayName(TextStyle.SHORT, ESPANOL).substring(0, 2), SwingConstants.CENTER);
            nombre.setForeground(Color.GRAY);
            dias.add(nombre);
        }
        int espacios=mesVisible.atDay(1).getDayOfWeek().getValue()-1;
        for (int i = 0; i < espacios; i++) {
            dias.add(new JLabel(""));
        }
        LocalDate seleccionada=getFecha();
        LocalDate hoy=LocalDate.now();
        for (int d = 1; d <= mesVisible.lengthOfMonth(); d++) {
            LocalDate fecha=mesVisible.atDay(d);
            JButton boton=new JButton(String.valueOf(d));
            boton.setMargin(new Insets(2, 2, 2, 2));
            boton.setPreferredSize(new Dimension(38, 26));
            boton.setFocusable(false);
            if(fecha.equals(seleccionada)){
                boton.setFont(boton.getFont().deriveFont(Font.BOLD));
                boton.setBackground(new Color(190, 215, 245));
            }
            if(fecha.equals(hoy)){
                boton.setForeground(new Color(0, 90, 180));
                boton.setToolTipText("Hoy");
            }
            boton.addActionListener(e -> Seleccionar(fecha));
            dias.add(boton);
        }
        calendario.add(dias, BorderLayout.CENTER);

        JButton botonHoy=new JButton("Hoy");
        botonHoy.setFocusable(false);
        botonHoy.addActionListener(e -> Seleccionar(LocalDate.now()));
        calendario.add(botonHoy, BorderLayout.SOUTH);

        popup.add(calendario);
        popup.pack();
    }

    private JButton BotonNavegar(String texto, String ayuda, int meses){
        JButton boton=new JButton(texto);
        boton.setToolTipText(ayuda);
        boton.setMargin(new Insets(1, 6, 1, 6));
        boton.setFocusable(false);
        boton.addActionListener(e -> {
            mesVisible=mesVisible.plusMonths(meses);
            ConstruirCalendario();
            popup.revalidate();
            popup.repaint();
        });
        return boton;
    }

    private void Seleccionar(LocalDate fecha){
        setFecha(fecha);
        popup.setVisible(false);
        texto.requestFocus();
    }
}
