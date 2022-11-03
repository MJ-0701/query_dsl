package com.example.querydsl.entity.repository;

import com.example.querydsl.controller.dto.MemberSearchCondition;
import com.example.querydsl.controller.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);
}
