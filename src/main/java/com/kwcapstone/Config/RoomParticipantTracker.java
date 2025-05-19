package com.kwcapstone.Config;

import com.kwcapstone.Domain.Dto.Request.ParticipantDto;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Service.MemberService;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class RoomParticipantTracker {
    private final Map<String, Set<String>> participants = new ConcurrentHashMap<>();
    private final MemberRepository memberRepository;

    public RoomParticipantTracker(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void addParticipant(String projectId, String memberId) {
        participants.computeIfAbsent(projectId, k -> ConcurrentHashMap.newKeySet()).add(memberId);
    }

    public void removeParticipant(String projectId, String memberId) {
        participants.getOrDefault(projectId, Set.of()).removeIf(p -> p.equals(memberId));
    }

    public Set<String> getParticipantIds(String projectId) {
        return participants.getOrDefault(projectId, Set.of());
    }

    public List<ParticipantDto> getParticipantDtos(String projectId) {
        return getParticipantIds(projectId).stream()
                .map(memberId -> {
                    ObjectId objectId = new ObjectId(memberId);
                    Member member = memberRepository.findByMemberId(objectId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "해당 ID의 멤버를 찾을 수 없습니다"));
                    return new ParticipantDto(
                            member.getMemberId().toHexString(),
                            member.getName(),
                            member.getImage()
                    );
                }).collect(Collectors.toList());
    }
}
