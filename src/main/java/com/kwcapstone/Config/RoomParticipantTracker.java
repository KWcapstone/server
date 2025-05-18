package com.kwcapstone.Config;

import com.kwcapstone.Domain.Dto.Request.ParticipantRequestDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomParticipantTracker {
    private final Map<String, Set<ParticipantRequestDto>> participants = new ConcurrentHashMap<>();

    public void addParticipant(String projectId, ParticipantRequestDto participantDto) {
        participants.computeIfAbsent(projectId, k -> ConcurrentHashMap.newKeySet()).add(participantDto);
    }

    public void removeParticipant(String projectId, String memberId) {
        participants.getOrDefault(projectId, Set.of()).removeIf(p -> p.getMemberId().equals(memberId));
    }

    public Set<ParticipantRequestDto> getParticipants(String conferenceId) {
        return participants.getOrDefault(conferenceId, Set.of());
    }
}
