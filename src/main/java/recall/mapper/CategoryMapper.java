package recall.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import recall.domain.Category;
import recall.dto.CategoryDto;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    List<CategoryDto> toDtoList(List<Category> categories);
}
