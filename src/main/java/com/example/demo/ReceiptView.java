package com.example.demo;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Route("")
public class ReceiptView extends HorizontalLayout {

    private final ChatClient client;
    private byte[] imageBytes;
    private String mimeType;
    private final VerticalLayout resultsLayout;
    private final Image imagePreview;
    private final Button analyzeButton;
    private final Div timerDisplay;
    private final AtomicBoolean timerRunning = new AtomicBoolean(false);

    public ReceiptView(ChatClient.Builder builder) {
        this.client = builder.build();

        setWidthFull();
        setHeightFull();
        setPadding(true);
        setSpacing(true);

        // Initialize components used in listeners first
        imagePreview = new Image();
        imagePreview.setMaxWidth("100%");
        imagePreview.setVisible(false);

        resultsLayout = new VerticalLayout();
        resultsLayout.setPadding(false);

        analyzeButton = new Button("Analyze");
        analyzeButton.setEnabled(false);

        // Create custom timer display
        timerDisplay = new Div();
        timerDisplay.setText("00:00.00");
        timerDisplay.setVisible(false);
        timerDisplay.getStyle()
                .set("position", "fixed")
                .set("top", "20px")
                .set("right", "20px")
                .set("background-color", "yellow")
                .set("color", "white")
                .set("padding", "15px 30px")
                .set("border-radius", "8px")
                .set("font-weight", "bold")
                .set("font-size", "20px")
                .set("box-shadow", "0 4px 6px rgba(0,0,0,0.3)")
                .set("z-index", "9999");

        // Left side: Controls
        VerticalLayout controlsLayout = new VerticalLayout();
        controlsLayout.setWidth("40%");
        controlsLayout.setHeightFull();
        controlsLayout.getStyle()
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("padding", "20px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/*");

        upload.addSucceededListener(e -> {
            try {
                imageBytes = buffer.getInputStream().readAllBytes();
                mimeType = e.getMIMEType();

                StreamResource resource = new StreamResource(e.getFileName(),
                        () -> new ByteArrayInputStream(imageBytes));
                imagePreview.setSrc(resource);
                imagePreview.setVisible(true);

                analyzeButton.setEnabled(true);
                resultsLayout.removeAll(); // Clear previous results
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        analyzeButton.addClickListener(e -> analyzeImage());

        controlsLayout.add(new H3("Upload Receipt"), upload, analyzeButton);

        // Right side: Preview and Results
        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setWidth("60%");
        rightLayout.setHeightFull();
        rightLayout.getStyle()
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("padding", "20px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)")
                .set("overflow", "auto"); // Allow scrolling if content is long

        rightLayout.add(imagePreview, resultsLayout);

        add(controlsLayout, rightLayout, timerDisplay);
    }

    private void analyzeImage() {
        if (imageBytes == null)
            return;

        // Disable button and show timer
        analyzeButton.setEnabled(false);
        timerDisplay.setVisible(true);
        timerDisplay.setText("00:00.00");
        timerRunning.set(true);

        // Store UI instance
        UI ui = UI.getCurrent();

        // Start timer update thread
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            while (timerRunning.get()) {
                long elapsed = System.currentTimeMillis() - startTime;
                long seconds = elapsed / 1000;
                long centiseconds = (elapsed % 1000) / 10;
                String timeText = String.format("%02d:%02d.%02d", seconds / 60, seconds % 60, centiseconds);

                ui.access(() -> {
                    timerDisplay.setText(timeText);
                    ui.push();
                });

                try {
                    Thread.sleep(50); // Update every 50ms
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        // Run AI analysis in background
        CompletableFuture.runAsync(() -> {
            try {
                var receipt = client.prompt()
                        .user(userMessage -> userMessage
                                .text("""
                                        Please read the attached receipt and return the value in provided format
                                        """)
                                .media(
                                        MimeTypeUtils.parseMimeType(mimeType),
                                        new ByteArrayResource(imageBytes)))
                        .call()
                        .entity(Receipt.class);

                ui.access(() -> {
                    showReceipt(receipt);
                    stopTimer();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                ui.access(() -> {
                    resultsLayout.add(new Paragraph("Error analyzing image: " + ex.getMessage()));
                    stopTimer();
                });
            }
        });
    }

    private void stopTimer() {
        timerRunning.set(false);
        timerDisplay.setVisible(false);
        analyzeButton.setEnabled(true);
    }

    private void showReceipt(Receipt receipt) {
        resultsLayout.removeAll();

        var items = new Grid<>(LineItem.class);
        items.setItems(receipt.lineItems());

        resultsLayout.add(
                new H3("Receipt details"),
                new Paragraph("Merchant: " + receipt.merchant()),
                new Paragraph("Total: " + receipt.total()),
                items);
    }
}
