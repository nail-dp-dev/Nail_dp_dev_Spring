package com.backend.naildp.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.naildp.dto.chat.ChatListSummaryResponse;
import com.backend.naildp.dto.chat.ChatMessageDto;
import com.backend.naildp.dto.chat.ChatRoomRequestDto;
import com.backend.naildp.dto.chat.MessageSummaryResponse;
import com.backend.naildp.dto.chat.RenameChatRoomRequestDto;
import com.backend.naildp.exception.ApiResponse;
import com.backend.naildp.oauth2.impl.UserDetailsImpl;
import com.backend.naildp.service.chat.ChatRoomStatusService;
import com.backend.naildp.service.chat.ChatService;
import com.backend.naildp.service.chat.KafkaProducerService;
import com.backend.naildp.service.chat.MessageStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "Chat")
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class ChatController {
	private final ChatService chatService;
	private final SimpMessagingTemplate simpMessagingTemplate;
	private final KafkaProducerService kafkaProducerService;
	private final ChatRoomStatusService chatRoomStatusService;
	private final MessageStatusService messageStatusService;

	@PostMapping("/chat")
	public ResponseEntity<ApiResponse<?>> createChatRoom(@AuthenticationPrincipal UserDetailsImpl userDetails,
		@RequestBody ChatRoomRequestDto chatRoomRequestDto) {
		UUID chatRoomId = chatService.createChatRoom(userDetails.getUser().getNickname(), chatRoomRequestDto);
		return ResponseEntity.ok(ApiResponse.successResponse(chatRoomId, "채팅방 생성 성공", 2001));
	}

	@MessageMapping("/chat/{chatRoomId}/message")
	public ResponseEntity<ApiResponse<?>> sendMessage(ChatMessageDto chatMessageDto,
		@DestinationVariable("chatRoomId") UUID chatRoomId) {
		chatService.sendMessage(chatMessageDto, chatRoomId);

		log.info("Message [{}] sent by user: {} to chatting room: {}", chatMessageDto.getContent(),
			chatMessageDto.getSender(), chatRoomId);
		return ResponseEntity.ok(ApiResponse.successResponse(null, "메시지 전송 성공", 2000));

	}

	@GetMapping("/chat/list")
	public ResponseEntity<ApiResponse<?>> getChatList(@AuthenticationPrincipal UserDetailsImpl userDetails,
		@RequestParam(required = false, defaultValue = "all", value = "category") String category,
		@RequestParam(required = false, defaultValue = "20", value = "size") int size,
		@RequestParam(required = false, value = "cursorId") UUID cursorId) {
		ChatListSummaryResponse response = chatService.getChatList(userDetails.getUser().getNickname(), category, size,
			cursorId);
		return ResponseEntity.ok(ApiResponse.successResponse(response, "채팅방 목록 조회 성공", 2000));
	}

	@GetMapping("/chat/{chatRoomId}")
	public ResponseEntity<ApiResponse<?>> getMessagesByRoomId(@PathVariable("chatRoomId") UUID chatRoomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		MessageSummaryResponse messageResponseDto = chatService.getMessagesByRoomId(chatRoomId,
			userDetails.getUser().getNickname());
		return ResponseEntity.ok(ApiResponse.successResponse(messageResponseDto, "특정 메시지 조회 성공", 2000));
	}

	@PostMapping("/chat/{chatRoomId}/images")
	public ResponseEntity<ApiResponse<?>> sendImageMessages(@PathVariable("chatRoomId") UUID chatRoomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestPart("images") List<MultipartFile> imageFiles) {

		chatService.sendImageMessages(chatRoomId, userDetails.getUser().getNickname(),
			imageFiles);
		return ResponseEntity.ok(ApiResponse.successResponse(null, "이미지 메시지 전송 성공", 2001));

	}

	@PostMapping("/chat/{chatRoomId}/video")
	public ResponseEntity<ApiResponse<?>> sendVideoMessage(@PathVariable("chatRoomId") UUID chatRoomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestPart("video") MultipartFile video) {

		chatService.sendVideoMessage(chatRoomId, userDetails.getUser().getNickname(),
			video);

		return ResponseEntity.ok(ApiResponse.successResponse(null, "동영상 메시지 전송 성공", 2001));

	}

	@PostMapping("/chat/{chatRoomId}/file")
	public ResponseEntity<ApiResponse<?>> sendFileMessages(@PathVariable("chatRoomId") UUID chatRoomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestPart("file") MultipartFile video) {

		chatService.sendFileMessage(chatRoomId, userDetails.getUser().getNickname(),
			video);
		return ResponseEntity.ok(ApiResponse.successResponse(null, "파일 메시지 전송 성공", 2001));

	}

	@DeleteMapping("/chat/{chatRoomId}/leave")
	public ResponseEntity<ApiResponse<?>> leaveChatRoom(@PathVariable("chatRoomId") UUID chatRoomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		chatService.leaveChatRoom(chatRoomId, userDetails.getUser().getNickname());
		return ResponseEntity.ok(ApiResponse.successResponse(null, "채팅방에서 나갔습니다", 2001));
	}

	@PatchMapping("/chat/{chatRoomId}/pinning")
	public ResponseEntity<ApiResponse<?>> pinByChatRoomUser(@PathVariable("chatRoomId") UUID chatRoomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		chatService.pinByChatRoomUser(chatRoomId, userDetails.getUser().getNickname());
		return ResponseEntity.ok(ApiResponse.successResponse(null, "해당 채팅방을 고정했습니다", 2001));

	}

	@PatchMapping("/chat/{chatRoomId}/unpinning")
	public ResponseEntity<ApiResponse<?>> unpinByChatRoomUser(@PathVariable("chatRoomId") UUID chatRoomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		chatService.unpinByChatRoomUser(chatRoomId, userDetails.getUser().getNickname());
		return ResponseEntity.ok(ApiResponse.successResponse(null, "해당 채팅방을 고정 해제했습니다", 2001));

	}

	@PatchMapping("/chat/{chatRoomId}")
	public ResponseEntity<ApiResponse<?>> renameChatRoom(@PathVariable("chatRoomId") UUID chatRoomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody RenameChatRoomRequestDto request) {
		chatService.renameChatRoom(chatRoomId, userDetails.getUser().getNickname(), request);
		return ResponseEntity.ok(ApiResponse.successResponse(null, "해당 채팅방을 이름을 변경했습니다", 2001));
	}
}
