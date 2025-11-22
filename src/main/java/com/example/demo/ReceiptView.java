package com.example.demo;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.MimeTypeUtils;

@Route("")
public class ReceiptView extends VerticalLayout {

    public ReceiptView(ChatClient.Builder builder) {
        var client = builder.build();
        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/*");
        upload.addSucceededListener(e -> {
            try {
                var receipt = client.prompt()
                        .user(userMessage -> userMessage
                                .text("""
                                        Please read the attached receipt and return the value in provided format
                                        """)
                                .media(
                                        MimeTypeUtils.parseMimeType(e.getMIMEType()),
                                        new InputStreamResource(buffer.getInputStream())))
                        .call()
                        .entity(Receipt.class);
                showReceipt(receipt);
                upload.clearFileList();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        add(upload);
    }

    private void showReceipt(Receipt receipt) {
        var items = new Grid<>(LineItem.class);
        items.setItems(receipt.lineItems());
        add(
                new H3("Receipt details"),
                new Paragraph("Merchant: " + receipt.merchant()),
                new Paragraph("Total: " + receipt.total()),
                items);
    }
}
