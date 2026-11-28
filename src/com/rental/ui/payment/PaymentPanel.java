package com.rental.ui.payment;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.enums.PaymentMethod;
import com.rental.exception.ValidationException;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.service.PaymentService;
import com.rental.service.RentalService;
import com.rental.ui.common.Refreshable;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import com.rental.util.Formats;

public class PaymentPanel extends JPanel implements Refreshable {
    private static final String PAGE_LIST = "LIST";
    private static final String PAGE_PROCESS = "PROCESS";
    private static final String FILTER_ALL = "All Agreements";
    private static final String FILTER_DUE = "Balance Due";
    private static final String FILTER_PAID = "Paid in Full";

    private final PaymentService paymentService;
    private final RentalService rentalService;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);

    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> filterBox = new JComboBox<>(new String[]{FILTER_ALL, FILTER_DUE, FILTER_PAID});
    private final DefaultTableModel model = UiUtil.tableModel("Agreement ID", "Customer", "Vehicle", "Status", "Outstanding");
    private final JTable table = UiUtil.table(model);
    private final JButton proceedBtn = UiTheme.primaryButton("Proceed to Payment");
    private List<RentalAgreement> current = new ArrayList<>();

    private final JTextArea details = UiUtil.readOnlyArea(10, 40);
    private final JTextField amountField = new JTextField(14);
    private final JTextField dateField = new JTextField(14);
    private final JComboBox<PaymentMethod> methodBox = new JComboBox<>(PaymentMethod.values());
    private final JLabel processMessage = UiUtil.messageLabel();
    private RentalAgreement selectedAgreement;

    public PaymentPanel(PaymentService paymentService, RentalService rentalService) {
        this.paymentService = paymentService;
        this.rentalService = rentalService;
        setLayout(new BorderLayout());

        cardContainer.add(buildListPage(), PAGE_LIST);
        cardContainer.add(buildProcessPage(), PAGE_PROCESS);
        add(cardContainer, BorderLayout.CENTER);

        loadList();
        UiTheme.style(this);
    }

    private JPanel buildListPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.add(UiUtil.pageHeader("Payments & Receipts", "Select a returned agreement to process a payment."), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, UiTheme.PRIMARY),
                BorderFactory.createEmptyBorder(20, 26, 20, 26)));

        JButton searchBtn = UiTheme.primaryButton("Search");
        JButton allBtn = UiTheme.secondaryButton("Show All");
        searchBtn.addActionListener(e -> loadList());
        searchField.addActionListener(e -> loadList());
        filterBox.addActionListener(e -> loadList());
        allBtn.addActionListener(e -> { searchField.setText(""); filterBox.setSelectedIndex(0); loadList(); });
        
        body.add(UiUtil.card(UiUtil.flow(new JLabel("Search:"), searchField, new JLabel("Filter:"), filterBox, searchBtn, allBtn), 14), BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> proceedBtn.setEnabled(table.getSelectedRow() >= 0));
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) openProcessPage(); }
        });

        JPanel tableCardInner = new JPanel(new BorderLayout(0, 12));
        tableCardInner.setOpaque(false);
        tableCardInner.add(new JScrollPane(table), BorderLayout.CENTER);
        
        proceedBtn.setEnabled(false);
        proceedBtn.addActionListener(e -> openProcessPage());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        footer.add(UiUtil.flowRight(proceedBtn), BorderLayout.EAST);
        
        tableCardInner.add(footer, BorderLayout.SOUTH);
        body.add(UiUtil.card(tableCardInner, 14), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildProcessPage() {
        JPanel page = new JPanel(new BorderLayout());
        
        JButton backBtn = UiTheme.secondaryButton("< Back to List");
        backBtn.addActionListener(e -> { cards.show(cardContainer, PAGE_LIST); loadList(); });
        JButton printBtn = UiTheme.secondaryButton("Print Receipt");
        printBtn.addActionListener(e -> reprint());
        
        page.add(UiUtil.pageHeader("Record Payment", "Process payments for this agreement.", backBtn, printBtn), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(24, 0));
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, UiTheme.PRIMARY),
                BorderFactory.createEmptyBorder(20, 26, 20, 26)));

        JPanel leftPanel = UiUtil.titled("Summary & History", new JScrollPane(details));

        dateField.setText(LocalDate.now().toString());
        JButton fillFullBtn = UiTheme.secondaryButton("Fill Balance");
        fillFullBtn.addActionListener(e -> { if (selectedAgreement != null && selectedAgreement.isReturned()) amountField.setText(String.format("%.2f", selectedAgreement.balanceDue())); });
        
        JPanel amountRow = new JPanel(new BorderLayout(10, 0));
        amountRow.setOpaque(false);
        amountRow.add(amountField, BorderLayout.CENTER);
        amountRow.add(fillFullBtn, BorderLayout.EAST);

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        UiUtil.addRow(formGrid, 0, "Amount *", amountRow);
        UiUtil.addRow(formGrid, 1, "Date *", dateField);
        UiUtil.addRow(formGrid, 2, "Method *", methodBox);

        JButton payBtn = UiTheme.primaryButton("Record Payment");
        payBtn.addActionListener(e -> recordPayment());

        JPanel formWrap = new JPanel(new BorderLayout(0, 10));
        formWrap.setOpaque(false);
        formWrap.add(formGrid, BorderLayout.NORTH);
        formWrap.add(processMessage, BorderLayout.CENTER);
        formWrap.add(UiUtil.flowRight(payBtn), BorderLayout.SOUTH);

        JPanel rightPanel = UiUtil.card(UiUtil.titled("Payment Details", formWrap), 14);
        rightPanel.setPreferredSize(new Dimension(450, 0));

        body.add(leftPanel, BorderLayout.CENTER);
        body.add(rightPanel, BorderLayout.EAST);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void loadList() {
        String filter = (String) filterBox.getSelectedItem();
        current = new ArrayList<>();
        for (RentalAgreement a : rentalService.search(searchField.getText(), false)) {
            if (FILTER_DUE.equals(filter) && !(a.isReturned() && a.balanceDue() > 0.001)) continue;
            if (FILTER_PAID.equals(filter) && !(a.isReturned() && a.balanceDue() <= 0.001)) continue;
            current.add(a);
        }
        model.setRowCount(0);
        for (RentalAgreement a : current) {
            model.addRow(new Object[]{ a.getAgreementId(), a.getReservation().getCustomer().getName(), a.getReservation().getVehicle().getRegistrationNumber(), a.getStatus(), a.isReturned() ? Formats.money(a.balanceDue()) : "-" });
        }
        table.clearSelection(); proceedBtn.setEnabled(false);
    }

    private void openProcessPage() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);
        for (RentalAgreement a : current) if (a.getAgreementId().equals(id)) { selectedAgreement = a; break; }
        if (selectedAgreement == null) return;

        UiUtil.clear(processMessage);
        amountField.setText(""); dateField.setText(LocalDate.now().toString());
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Agreement : %s%nCustomer  : %s%nVehicle   : %s%n", selectedAgreement.getAgreementId(), selectedAgreement.getReservation().getCustomer().getName(), selectedAgreement.getReservation().getVehicle().getRegistrationNumber()));
        sb.append(String.format("Final amount : %.2f%nPaid so far   : %.2f%nBalance      : %.2f%n%n", selectedAgreement.getFinalAmount(), selectedAgreement.totalPaid(), selectedAgreement.balanceDue()));
        for (Payment p : selectedAgreement.getPayments()) sb.append(String.format("  %s  %s  %.2f%n", p.getPaymentDate(), p.getMethod(), p.getAmountPaid()));
        details.setText(sb.toString());

        cards.show(cardContainer, PAGE_PROCESS);
    }

    private void recordPayment() {
        Payment[] saved = new Payment[1];
        boolean ok = UiUtil.run(this, processMessage, () -> {
            if (selectedAgreement == null) throw new ValidationException("No agreement selected.");
            Double amount = UiUtil.parseNumber(amountField.getText(), "Amount");
            LocalDate date = UiUtil.parseDate(dateField.getText(), "Date");
            saved[0] = paymentService.recordPayment(selectedAgreement, amount, date, (PaymentMethod) methodBox.getSelectedItem());
        });
        if (ok) {
            UiUtil.showOk(processMessage, "Payment saved. Balance: " + Formats.money(selectedAgreement.balanceDue()));
            UiUtil.showPrintPreview(this, "Receipt", paymentService.renderInvoice(selectedAgreement) + "\n" + paymentService.renderReceipt(saved[0]));
            openProcessPage();
        }
    }

    private void reprint() {
        if (selectedAgreement != null) UiUtil.showPrintPreview(this, "Invoice", paymentService.renderInvoice(selectedAgreement));
    }

    @Override public void refresh() { loadList(); }
}