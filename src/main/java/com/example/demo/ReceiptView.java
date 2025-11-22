package com.example.demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
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

@Route("")
public class ReceiptView extends HorizontalLayout {

    private final ChatClient client;
    private byte[] imageBytes;
    private String mimeType;
    private final VerticalLayout resultsLayout;
    private final Image imagePreview;
    private final Button analyzeButton;

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

        add(controlsLayout, rightLayout);
    }

    private void analyzeImage() {
        if (imageBytes == null)
            return;

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
            showReceipt(receipt);
        } catch (Exception ex) {
            ex.printStackTrace();
            resultsLayout.add(new Paragraph("Error analyzing image: " + ex.getMessage()));
        }
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
