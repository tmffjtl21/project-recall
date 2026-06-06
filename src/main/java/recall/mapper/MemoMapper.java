package recall.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import recall.domain.Memo;
import recall.dto.MemoDto;

@Mapper(componentModel = "spring")
public interface MemoMapper {

    @Mapping(source = "createdBy", target = "writer")
    @Mapping(source = "createdDate", target = "createdAt")
    @Mapping(source = "lastModifiedDate", target = "updatedAt")
    MemoDto toDto(Memo memo);

    List<MemoDto> toDtoList(List<Memo> memos);
}
