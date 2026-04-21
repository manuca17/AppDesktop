package com.example.appdesktop;

import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Reuniao;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import com.example.appdesktop.services.ReuniaoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AdminMeetingsController implements AdminPage {

    @FXML private Label totalMeetingsLabel;
    @FXML private Label scheduledMeetingsLabel;
    @FXML private Label confirmedMeetingsLabel;
    @FXML private Label cancelledMeetingsLabel;
    @FXML private VBox upcomingMeetingsContainer;
    @FXML private VBox pastMeetingsContainer;
    @FXML private Label emptyUpcomingLabel;
    @FXML private Label emptyPastLabel;

    private final ProjetoPersonalizadoService projetoService = ProjetoPersonalizadoService.getInstance();
    private final ReuniaoService reuniaoService = ReuniaoService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "PT"));

    private final List<MeetingRow> meetings = new ArrayList<>();
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        loadMeetings();
    }

    @Override
    public void setNavigator(AdminPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onAddMeeting() {
        Dialog<Button> dialog = new Dialog<>();
        dialog.setTitle("Agendar Reuniao");

        Button saveButton = new Button("Agendar");
        saveButton.setDefaultButton(true);
        Button cancelButton = new Button("Cancelar");
        dialog.getDialogPane().getButtonTypes().clear();
        HBox buttons = new HBox(8, cancelButton, saveButton);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        TextField clientField = new TextField();
        clientField.setPromptText("Nome do cliente");

        TextField projectField = new TextField();
        projectField.setPromptText("ID do projeto (ex: PRJ-10)");

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        TextField timeField = new TextField();
        timeField.setPromptText("14:00");

        TextArea notesField = new TextArea();
        notesField.setPromptText("Objetivo da reuniao...");
        notesField.setPrefRowCount(3);

        VBox form = new VBox(10,
                new Label("Cliente"), clientField,
                new Label("Projeto"), projectField,
                new Label("Data"), datePicker,
                new Label("Hora"), timeField,
                new Label("Notas"), notesField,
                buttons
        );
        form.setPadding(new Insets(10));

        cancelButton.setOnAction(e -> dialog.close());
        saveButton.setOnAction(e -> {
            String client = clientField.getText() == null ? "Cliente" : clientField.getText().trim();
            String project = projectField.getText() == null ? "PRJ-?" : projectField.getText().trim();
            String hora = timeField.getText() == null ? "10:00" : timeField.getText().trim();
            String notas = notesField.getText() == null ? "Sem notas" : notesField.getText().trim();

            if (client.isBlank() || project.isBlank()) {
                showInfo("Preencha cliente e projeto para agendar a reuniao.");
                return;
            }

            meetings.add(new MeetingRow(
                    "MEET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                    project,
                    client,
                    datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue(),
                    hora.isBlank() ? "10:00" : hora,
                    "scheduled",
                    notas.isBlank() ? "Sem notas" : notas
            ));
            refreshUI();
            dialog.close();
            showInfo("Reuniao agendada com sucesso.");
        });

        dialog.getDialogPane().setContent(form);
        dialog.showAndWait();
    }

    private void loadMeetings() {
        projetoService.findAll().whenComplete((projects, projectsError) -> {
            if (projectsError != null || projects == null || projects.isEmpty()) {
                Platform.runLater(() -> {
                    meetings.clear();
                    refreshUI();
                });
                return;
            }

            List<CompletableFuture<List<Reuniao>>> requests = new ArrayList<>();
            List<ProjetoPersonalizado> requestedProjects = new ArrayList<>();
            for (ProjetoPersonalizado project : projects) {
                if (project == null || project.getId() == null) {
                    continue;
                }
                requestedProjects.add(project);
                requests.add(reuniaoService.findByProjetoId(project.getId()).exceptionally(error -> List.of()));
            }

            CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new))
                    .whenComplete((done, error) -> Platform.runLater(() -> {
                        meetings.clear();

                        for (int i = 0; i < requests.size(); i++) {
                            ProjetoPersonalizado project = requestedProjects.get(i);
                            List<Reuniao> projectMeetings = requests.get(i).join();
                            String projectId = project.getId() == null ? "PRJ-?" : "PRJ-" + project.getId();
                            String clientName = project.getIdUtilizador() != null
                                    && project.getIdUtilizador().getNomeEmpresa() != null
                                    && !project.getIdUtilizador().getNomeEmpresa().isBlank()
                                    ? project.getIdUtilizador().getNomeEmpresa()
                                    : "Cliente";

                            for (Reuniao reuniao : projectMeetings) {
                                if (reuniao == null) {
                                    continue;
                                }
                                meetings.add(new MeetingRow(
                                        reuniao.getId() == null ? "MEET-?" : "MEET-" + reuniao.getId(),
                                        projectId,
                                        clientName,
                                        reuniao.getData() == null ? LocalDate.now() : reuniao.getData(),
                                        reuniao.getHora() == null ? "10:00" : reuniao.getHora(),
                                        normalizeMeetingStatus(reuniao.getEstado()),
                                        reuniao.getNotas() == null ? "Sem notas" : reuniao.getNotas()
                                ));
                            }
                        }

                        refreshUI();
                    }));
        });
    }

    private void refreshUI() {
        totalMeetingsLabel.setText(String.valueOf(meetings.size()));
        scheduledMeetingsLabel.setText(String.valueOf(meetings.stream().filter(m -> "scheduled".equals(m.status())).count()));
        confirmedMeetingsLabel.setText(String.valueOf(meetings.stream().filter(m -> "confirmed".equals(m.status())).count()));
        cancelledMeetingsLabel.setText(String.valueOf(meetings.stream().filter(m -> "cancelled".equals(m.status())).count()));

        List<MeetingRow> upcoming = meetings.stream()
                .filter(m -> !"cancelled".equals(m.status()))
                .filter(m -> !m.date().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(MeetingRow::date).thenComparing(MeetingRow::time))
                .toList();

        List<MeetingRow> past = meetings.stream()
                .filter(m -> "cancelled".equals(m.status()) || m.date().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(MeetingRow::date).reversed())
                .toList();

        renderUpcoming(upcoming);
        renderPast(past);
    }

    private void renderUpcoming(List<MeetingRow> rows) {
        upcomingMeetingsContainer.getChildren().clear();
        emptyUpcomingLabel.setVisible(rows.isEmpty());
        emptyUpcomingLabel.setManaged(rows.isEmpty());

        for (MeetingRow meeting : rows) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

            HBox head = new HBox(8);
            Label client = new Label(meeting.clientName());
            client.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label status = new Label(statusLabel(meeting.status()));
            status.setStyle(statusStyle(meeting.status()));
            head.getChildren().addAll(client, spacer, status);

            Label notes = new Label(meeting.notes());
            notes.setWrapText(true);
            notes.setStyle("-fx-text-fill: #4b5563;");

            Label meta = new Label(meeting.projectId() + "  |  " + dateFormatter.format(meeting.date()) + "  |  " + meeting.time());
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

            HBox actions = new HBox(8);
            if (!"cancelled".equals(meeting.status())) {
                Button cancel = new Button("Cancelar");
                cancel.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
                cancel.setOnAction(e -> cancelMeeting(meeting));
                actions.getChildren().add(cancel);
            }

            Button reschedule = new Button("Remarcar");
            reschedule.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
            reschedule.setOnAction(e -> rescheduleMeeting(meeting));
            actions.getChildren().add(reschedule);

            card.getChildren().addAll(head, notes, meta, actions);
            upcomingMeetingsContainer.getChildren().add(card);
        }
    }

    private void renderPast(List<MeetingRow> rows) {
        pastMeetingsContainer.getChildren().clear();
        emptyPastLabel.setVisible(rows.isEmpty());
        emptyPastLabel.setManaged(rows.isEmpty());

        for (MeetingRow meeting : rows) {
            VBox card = new VBox(6);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

            HBox head = new HBox(8);
            Label client = new Label(meeting.clientName());
            client.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label status = new Label(statusLabel(meeting.status()));
            status.setStyle(statusStyle(meeting.status()));
            head.getChildren().addAll(client, spacer, status);

            Label notes = new Label(meeting.notes());
            notes.setWrapText(true);
            notes.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

            Label meta = new Label(meeting.projectId() + "  |  " + dateFormatter.format(meeting.date()) + "  |  " + meeting.time());
            meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

            card.getChildren().addAll(head, notes, meta);
            pastMeetingsContainer.getChildren().add(card);
        }
    }

    private void updateMeetingStatus(String meetingId, String newStatus, String successMessage) {
        for (int i = 0; i < meetings.size(); i++) {
            MeetingRow row = meetings.get(i);
            if (row.id().equals(meetingId)) {
                meetings.set(i, new MeetingRow(row.id(), row.projectId(), row.clientName(), row.date(), row.time(), newStatus, row.notes()));
                break;
            }
        }
        refreshUI();
        showInfo(successMessage);
    }

    private void confirmMeeting(MeetingRow meeting) {
        Integer meetingId = extractMeetingNumericId(meeting.id());
        if (meetingId == null) {
            showInfo("ID da reuniao invalido.");
            return;
        }

        reuniaoService.confirmPresence(meetingId)
                .whenComplete((updated, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Nao foi possivel confirmar a reuniao.");
                        return;
                    }
                    updateMeetingStatus(meeting.id(), "confirmed", "Reuniao confirmada.");
                }));
    }

    private void cancelMeeting(MeetingRow meeting) {
        Integer meetingId = extractMeetingNumericId(meeting.id());
        if (meetingId == null) {
            showInfo("ID da reuniao invalido.");
            return;
        }

        reuniaoService.cancel(meetingId)
                .whenComplete((updated, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Nao foi possivel cancelar a reuniao.");
                        return;
                    }
                    updateMeetingStatus(meeting.id(), "cancelled", "Reuniao cancelada.");
                }));
    }

    private void rescheduleMeeting(MeetingRow meeting) {
        Integer meetingId = extractMeetingNumericId(meeting.id());
        if (meetingId == null) {
            showInfo("ID da reuniao invalido.");
            return;
        }

        Dialog<Button> dialog = new Dialog<>();
        dialog.setTitle("Remarcar Reuniao");

        Button saveButton = new Button("Remarcar");
        saveButton.setDefaultButton(true);
        Button cancelButton = new Button("Cancelar");
        dialog.getDialogPane().getButtonTypes().clear();
        HBox buttons = new HBox(8, cancelButton, saveButton);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        DatePicker datePicker = new DatePicker(meeting.date());
        TextField timeField = new TextField(meeting.time());
        timeField.setPromptText("HH:mm");

        TextField tipoField = new TextField();
        tipoField.setPromptText("videochamada");

        TextField localField = new TextField();
        localField.setPromptText("Google Meet");

        VBox form = new VBox(10,
                new Label("Data"), datePicker,
                new Label("Hora"), timeField,
                new Label("Tipo"), tipoField,
                new Label("Local"), localField,
                buttons
        );
        form.setPadding(new Insets(10));

        cancelButton.setOnAction(e -> dialog.close());
        saveButton.setOnAction(e -> {
            LocalDate data = datePicker.getValue();
            String hora = timeField.getText() == null ? "" : timeField.getText().trim();
            String tipo = tipoField.getText() == null ? "" : tipoField.getText().trim();
            String local = localField.getText() == null ? "" : localField.getText().trim();

            reuniaoService.reschedule(meetingId, data, hora, tipo, local)
                    .whenComplete((updated, error) -> Platform.runLater(() -> {
                        if (error != null) {
                            showInfo("Nao foi possivel remarcar a reuniao.");
                            return;
                        }
                        updateMeetingStatus(meeting.id(), "scheduled", "Reuniao remarcada.");
                        dialog.close();
                    }));
        });

        dialog.getDialogPane().setContent(form);
        dialog.showAndWait();
    }

    private Integer extractMeetingNumericId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String cleaned = rawId.trim().toUpperCase(Locale.ROOT);
        if (cleaned.startsWith("MEET-")) {
            cleaned = cleaned.substring(5);
        }
        if (cleaned.matches("\\d+")) {
            return Integer.parseInt(cleaned);
        }
        return null;
    }

    private String normalizeMeetingStatus(String status) {
        if (status == null || status.isBlank()) {
            return "scheduled";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "agendada", "scheduled" -> "scheduled";
            case "confirmada", "confirmed" -> "confirmed";
            case "cancelada", "cancelled" -> "cancelled";
            case "remarcada", "rescheduled" -> "scheduled";
            default -> "scheduled";
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "confirmed" -> "Confirmada";
            case "cancelled" -> "Cancelada";
            default -> "Agendada";
        };
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "confirmed" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "cancelled" -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reunioes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record MeetingRow(String id, String projectId, String clientName, LocalDate date, String time, String status,
                              String notes) {
    }
}