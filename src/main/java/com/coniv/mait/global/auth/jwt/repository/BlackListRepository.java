package com.coniv.mait.global.auth.jwt.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.coniv.mait.global.auth.jwt.BlackList;

@Repository
public interface BlackListRepository extends CrudRepository<BlackList, String> {
}
