package com.electro.controller.chat;

import com.electro.constant.AppConstants;
import com.electro.dto.ListResponse;
import com.electro.dto.chat.MessageRequest;
import com.electro.dto.chat.MessageResponse;
import com.electro.service.chat.MessageService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ChatController {
    private SimpMessagingTemplate simpMessagingTemplate;

    private MessageService messageService;

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, @Payload MessageRequest message) {
        MessageResponse msgResponse =  messageService.save(message);
        simpMessagingTemplate.convertAndSend("/chat/receive/" + roomId, msgResponse);
    }

    @GetMapping("/chat")
    public ResponseEntity<ListResponse<MessageResponse>> getAllProvinces(
            @RequestParam(name = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(name = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(name = "sort", defaultValue = AppConstants.DEFAULT_SORT) String sort,
            @RequestParam(name = "filter", required = false) @Nullable String filter,
            @RequestParam(name = "search", required = false) @Nullable String search,
            @RequestParam(name = "all", required = false) boolean all
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(messageService.findAll(page, size, sort, filter, search, all));
    }

}
