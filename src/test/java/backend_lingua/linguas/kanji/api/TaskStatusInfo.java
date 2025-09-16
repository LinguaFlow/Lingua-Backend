package backend_lingua.linguas.kanji.api;

import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;

public record TaskStatusInfo(TaskStatus taskStatus, String status, String responseBody){}
