package com.rental.ui;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import com.rental.AppContext;
import com.rental.enums.StaffRole;
import com.rental.model.*;
import com.rental.ui.common.Navigator;
import com.rental.ui.common.Refreshable;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import com.rental.util.Formats;

public class DashboardPanel extends JPanel implements Refreshable {
    private final AppContext ctx; private final Staff user; private final Navigator nav;
    private final JLabel k1 = new JLabel("0"), k2 = new JLabel("0"), k3 = new JLabel("0"), k4 = new JLabel("0");
    private final JLabel statusNote = new JLabel(" ");

    public DashboardPanel(AppContext ctx, Staff user, Navigator nav) {
        this.ctx = ctx; this.user = user; this.nav = nav;
        setLayout(new BorderLayout());
        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 16)); kpiGrid.setOpaque(false); kpiGrid.setAlignmentX(LEFT_ALIGNMENT);
        JPanel actionsGrid = new JPanel(new GridLayout(0, 3, 20, 20)); actionsGrid.setOpaque(false); actionsGrid.setAlignmentX(LEFT_ALIGNMENT);

        if (user.getRole() == StaffRole.RENTAL_STAFF) {
            kpiGrid.add(UiUtil.kpiCard("Customers", k1, UiTheme.PRIMARY)); kpiGrid.add(UiUtil.kpiCard("Reservations", k2, new Color(0x0284C7)));
            kpiGrid.add(UiUtil.kpiCard("Active Rentals", k3, UiTheme.SUCCESS)); kpiGrid.add(UiUtil.kpiCard("Unpaid", k4, UiTheme.WARNING));
            actionsGrid.add(tile("Customer Directory", UiTheme.secondaryButton("Open Directory"), () -> nav.navigateTo(Navigator.CUSTOMERS_DIRECTORY)));
            actionsGrid.add(tile("Register Customer", UiTheme.primaryButton("Register Customer"), () -> nav.navigateTo(Navigator.CUSTOMERS_CREATE)));
            actionsGrid.add(tile("Edit Customer", UiTheme.secondaryButton("Edit Customer"), () -> nav.navigateTo(Navigator.CUSTOMERS_EDIT)));
            actionsGrid.add(tile("New Reservation", UiTheme.secondaryButton("Create Reservation"), () -> nav.navigateTo(Navigator.RESERVATIONS_CREATE)));
            actionsGrid.add(tile("Vehicle Handover", UiTheme.secondaryButton("Go to Handover"), () -> nav.navigateTo(Navigator.HANDOVER)));
            actionsGrid.add(tile("Process Return", UiTheme.secondaryButton("Process Return"), () -> nav.navigateTo(Navigator.RETURN)));
        } else if (user.getRole() == StaffRole.FLEET_STAFF) {
            kpiGrid.add(UiUtil.kpiCard("Fleet Size", k1, UiTheme.PRIMARY)); kpiGrid.add(UiUtil.kpiCard("Available", k2, UiTheme.SUCCESS));
            kpiGrid.add(UiUtil.kpiCard("Maintenance", k3, UiTheme.WARNING)); kpiGrid.add(UiUtil.kpiCard("Flagged", k4, UiTheme.DANGER));
            actionsGrid.add(tile("Fleet Directory", UiTheme.secondaryButton("Open Fleet"), () -> nav.navigateTo(Navigator.FLEET_DIRECTORY)));
            actionsGrid.add(tile("Register Vehicle", UiTheme.primaryButton("Add New Vehicle"), () -> nav.navigateTo(Navigator.FLEET_ADD)));
            actionsGrid.add(tile("Maintenance", UiTheme.secondaryButton("Manage Maintenance"), () -> nav.navigateTo(Navigator.MAINTENANCE)));
        } else {
            kpiGrid.add(UiUtil.kpiCard("Vehicles", k1, UiTheme.PRIMARY)); kpiGrid.add(UiUtil.kpiCard("Customers", k2, new Color(0x0284C7)));
            kpiGrid.add(UiUtil.kpiCard("Agreements", k3, UiTheme.SUCCESS)); kpiGrid.add(UiUtil.kpiCard("Revenue", k4, UiTheme.WARNING));
            actionsGrid.add(tile("System Reports", UiTheme.primaryButton("Open Reports"), () -> nav.navigateTo(Navigator.REPORTS)));
        }

        body.add(kpiGrid); body.add(Box.createVerticalStrut(32));
        JLabel qaTitle = new JLabel("Quick Actions"); qaTitle.setFont(new Font("Segoe UI", Font.BOLD, 20)); qaTitle.setAlignmentX(LEFT_ALIGNMENT);
        body.add(qaTitle); body.add(Box.createVerticalStrut(16)); body.add(actionsGrid); body.add(Box.createVerticalStrut(24));
        statusNote.setAlignmentX(LEFT_ALIGNMENT); statusNote.setFont(UiTheme.LABEL_FONT); statusNote.setForeground(UiTheme.TEXT_MUTED); body.add(statusNote);

        add(UiUtil.pageHeader("Welcome, " + user.getName(), "Role: " + user.getRole()), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(body); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER); refresh(); UiTheme.style(this);
    }

    private JPanel tile(String title, JButton btn, Runnable onClick) {
        JPanel inner = new JPanel(new BorderLayout(0, 16)); inner.setOpaque(false);
        JLabel t = new JLabel(title); t.setFont(UiTheme.TITLE_FONT); inner.add(t, BorderLayout.CENTER);
        btn.addActionListener(e -> onClick.run()); inner.add(UiUtil.flow(btn), BorderLayout.SOUTH);
        return UiUtil.card(inner, 20);
    }

    @Override public void refresh() {
        if (user.getRole() == StaffRole.RENTAL_STAFF) {
            ctx.reservationService.expireOverdueReservations();
            k1.setText(String.valueOf(ctx.customerService.findAll().size()));
            k2.setText(String.valueOf(ctx.reservationService.findAll().stream().filter(r -> r.getStatus() == com.rental.enums.ReservationStatus.CONFIRMED).count()));
            List<RentalAgreement> ag = ctx.rentalService.findAll();
            k3.setText(String.valueOf(ag.stream().filter(RentalAgreement::isActive).count()));
            k4.setText(String.valueOf(ag.stream().filter(a -> a.isReturned() && a.balanceDue() > 0.001).count()));
            statusNote.setText("Date: " + LocalDate.now() + " | LVA: " + (ctx.lva.isAvailable() ? "Online" : "Outage"));
        } else if (user.getRole() == StaffRole.FLEET_STAFF) {
            List<Vehicle> all = ctx.vehicleService.findAll();
            k1.setText(String.valueOf(all.size()));
            k2.setText(String.valueOf(all.stream().filter(v -> v.getStatus() == com.rental.enums.VehicleStatus.AVAILABLE).count()));
            k3.setText(String.valueOf(all.stream().filter(v -> v.getStatus() == com.rental.enums.VehicleStatus.UNDER_MAINTENANCE).count()));
            k4.setText(String.valueOf(ctx.maintenanceService.findFlaggedVehicles().size()));
            statusNote.setText("All fleet statuses are up to date.");
        } else {
            k1.setText(String.valueOf(ctx.vehicleService.findAll().size()));
            k2.setText(String.valueOf(ctx.customerService.findAll().size()));
            k3.setText(String.valueOf(ctx.rentalService.findAll().size()));
            double rev = 0; for (RentalAgreement a : ctx.rentalService.findAll()) for (Payment p : a.getPayments()) rev += p.getAmountPaid();
            k4.setText(Formats.money(rev)); statusNote.setText("System reports cover all persisted records.");
        }
    }
}