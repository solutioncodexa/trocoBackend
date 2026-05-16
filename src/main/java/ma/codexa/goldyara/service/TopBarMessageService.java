package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.dto.TopBarMessageDTO;
import ma.codexa.goldyara.entity.TopBarMessage;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.repository.TopBarMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TopBarMessageService {
    
    private final TopBarMessageRepository topBarMessageRepository;
    
    public List<TopBarMessageDTO> getAllActiveMessages() {
        log.info("Récupération de tous les messages actifs de la top bar");
        return topBarMessageRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TopBarMessageDTO> getAllMessages() {
        log.info("Récupération de tous les messages de la top bar");
        return topBarMessageRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public TopBarMessageDTO createMessage(TopBarMessageDTO messageDTO) {
        log.info("Création d'un nouveau message de top bar: {}", messageDTO.getMessage());

        if (messageDTO.getMessage() == null || messageDTO.getMessage().isBlank()) {
            throw new IllegalArgumentException("Le message est obligatoire.");
        }
        if (messageDTO.getDisplayOrder() == null) {
            throw new IllegalArgumentException("L'ordre d'affichage est obligatoire.");
        }

        int order = messageDTO.getDisplayOrder();
        if (order < 1) {
            order = 1;
        }

        int duration = messageDTO.getDisplayDurationSeconds() != null ? messageDTO.getDisplayDurationSeconds() : 7;
        if (duration < 2) {
            duration = 2;
        }
        if (duration > 600) {
            duration = 600;
        }

        TopBarMessage message = new TopBarMessage();
        message.setMessage(messageDTO.getMessage().trim());
        message.setDisplayOrder(order);
        message.setIsActive(messageDTO.getIsActive() != null ? messageDTO.getIsActive() : true);
        message.setDisplayDurationSeconds(duration);

        TopBarMessage savedMessage = topBarMessageRepository.save(message);
        log.info("Message créé avec l'ID: {}", savedMessage.getId());
        
        return convertToDTO(savedMessage);
    }
    
    public TopBarMessageDTO updateMessage(Long id, TopBarMessageDTO messageDTO) {
        log.info("Mise à jour du message de top bar avec l'ID: {}", id);
        
        TopBarMessage existingMessage = topBarMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message de top bar", id));
        
        if (messageDTO.getMessage() == null || messageDTO.getMessage().isBlank()) {
            throw new IllegalArgumentException("Le message est obligatoire.");
        }
        existingMessage.setMessage(messageDTO.getMessage().trim());
        if (messageDTO.getDisplayOrder() == null) {
            throw new IllegalArgumentException("L'ordre d'affichage est obligatoire.");
        }
        int order = messageDTO.getDisplayOrder();
        existingMessage.setDisplayOrder(order < 1 ? 1 : order);
        if (messageDTO.getIsActive() != null) {
            existingMessage.setIsActive(messageDTO.getIsActive());
        }
        int duration =
                messageDTO.getDisplayDurationSeconds() != null
                        ? messageDTO.getDisplayDurationSeconds()
                        : (existingMessage.getDisplayDurationSeconds() != null
                                ? existingMessage.getDisplayDurationSeconds()
                                : 7);
        if (duration < 2) {
            duration = 2;
        }
        if (duration > 600) {
            duration = 600;
        }
        existingMessage.setDisplayDurationSeconds(duration);
        
        TopBarMessage updatedMessage = topBarMessageRepository.save(existingMessage);
        log.info("Message mis à jour avec succès");
        
        return convertToDTO(updatedMessage);
    }
    
    public void deleteMessage(Long id) {
        log.info("Suppression du message de top bar avec l'ID: {}", id);
        
        TopBarMessage message = topBarMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message de top bar", id));
        
        topBarMessageRepository.delete(message);
        log.info("Message supprimé avec succès");
    }
    
    public TopBarMessageDTO toggleActive(Long id, Boolean isActive) {
        log.info("Changement du statut actif du message {} à {}", id, isActive);
        
        TopBarMessage message = topBarMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message de top bar", id));
        
        message.setIsActive(isActive);
        TopBarMessage updatedMessage = topBarMessageRepository.save(message);
        
        return convertToDTO(updatedMessage);
    }
    
    private TopBarMessageDTO convertToDTO(TopBarMessage message) {
        TopBarMessageDTO dto = new TopBarMessageDTO();
        dto.setId(message.getId());
        dto.setMessage(message.getMessage());
        dto.setDisplayOrder(message.getDisplayOrder());
        dto.setIsActive(message.getIsActive());
        dto.setDisplayDurationSeconds(
                message.getDisplayDurationSeconds() != null ? message.getDisplayDurationSeconds() : 7);
        return dto;
    }
}
