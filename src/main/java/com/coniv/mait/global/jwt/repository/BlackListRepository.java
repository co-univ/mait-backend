package com.coniv.mait.global.jwt.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.coniv.mait.global.jwt.BlackList;

@Repository
public interface BlackListRepository extends CrudRepository<BlackList, String> {
}
