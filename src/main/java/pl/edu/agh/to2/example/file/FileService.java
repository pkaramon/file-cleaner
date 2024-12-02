package pl.edu.agh.to2.example.file;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {
    private final FileRepository fileRepository;
    private final FileSearch fileSearch;

    public FileService(FileRepository fileRepository, FileSearch fileSearch) {
        this.fileRepository = fileRepository;
        this.fileSearch = fileSearch;
    }


    @Transactional
    public void loadFromPath(String path, String pattern) {
        fileSearch.searchDirectory(path).forEach(file -> {
            File fileEntity = new File(file.getName(), file.getPath(), file.length(), file.lastModified());

            fileRepository.findByPath(file.getPath())
                    .ifPresentOrElse(
                            existingFile -> {
                                if (existingFile.getLastModified() != file.lastModified()) {
                                    existingFile.setLastModified(file.lastModified());
                                    existingFile.setSize(file.length());
                                    fileRepository.save(existingFile);
                                    System.out.println("File updated: " + existingFile.getName());
                                }
                                System.out.println("File already exists: " + existingFile.getName());
                            },
                            () -> {
                                fileRepository.save(fileEntity);
                                System.out.println("File saved: " + fileEntity.getName());

                            }
                    );
        });
    }

    public List<File> findLargestFiles(int limit) {
        return fileRepository.findLargestFiles(limit);
    }
}
