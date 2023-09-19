package com.yichi.onepiece.file.service;

import com.yichi.onepiece.file.model.FileObjectDao;
import com.yichi.onepiece.file.model.FileObjectEntity;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FileObjectService {

  private final FileObjectDao fileObjectDao;


  public FileObjectService(FileObjectDao fileObjectDao) {
    this.fileObjectDao = fileObjectDao;
  }

  public FileObjectEntity get(UUID fileId) {
    return fileObjectDao.findById(fileId).orElseThrow();
  }

  public void add(FileObjectEntity fileObjectEntity) {
    fileObjectDao.insert(fileObjectEntity);
  }
}
