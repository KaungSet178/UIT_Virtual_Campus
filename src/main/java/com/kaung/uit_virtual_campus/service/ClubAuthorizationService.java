package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ClubAuthorizationService {

    public boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equalsIgnoreCase(roleName));
    }

    public boolean isAdmin(User user) {
        return hasRole(user, "ADMIN");
    }

    public boolean isStudent(User user) {
        return hasRole(user, "STUDENT");
    }

    public boolean isClubLeader(User user) {
        return hasRole(user, "CLUB_LEADER");
    }

    public boolean canManageClub(User user, Club club) {
        return isAdmin(user) || club.getLeader().getUserId().equals(user.getUserId());
    }

    public void requireClubManager(User user, Club club) {
        if (!canManageClub(user, club)) {
            throw new AccessDeniedException("Only this club's leader or an admin can manage this club.");
        }
    }

    public void requireStudent(User user) {
        if (!isStudent(user)) {
            throw new AccessDeniedException("Only students can join clubs.");
        }
    }
}
