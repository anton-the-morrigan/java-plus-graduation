package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.entity.User;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto addUser(NewUserRequest newUserRequest) {
        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new ConflictException("Указанный адрес электронной почты уже зарегестрирован");
        }
        User user = userRepository.save(userMapper.toUser(newUserRequest));
        return userMapper.toUserDto(user);
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(PageRequest.of(from, size)).stream().map(userMapper::toUserDto).collect(Collectors.toList());
        } else {
            return userRepository.findAllByIdIn(ids, PageRequest.of(from, size)).stream().map(userMapper::toUserDto).collect(Collectors.toList());
        }
    }

    private void userValidator(User user) {
        if (user.getName().isBlank()) {
            throw new BadRequestException("Имя не может быть пустым");
        } else if (user.getName().length() < 2 || user.getName().length() > 250) {
            throw new BadRequestException("Длина имени не может быть меньше 2 и больше 250 символов");
        }

        if (user.getEmail().isBlank()) {
            throw new BadRequestException("Адрес электронной почты не может быть пустым");
        } else if (user.getEmail().length() < 6 || user.getEmail().length() > 254) {
            throw new BadRequestException("Длина адреса электронной почты не может быть меньше 6 и больше 254 символов");
        }

        int separator = user.getEmail().indexOf("@");
        if (user.getEmail().substring(0, separator).length() > 64) {
            throw new BadRequestException("Локальная часть адреса электронной почты не может быть больше 64 символов");
        }
        if (user.getEmail().substring(separator).length() > 64 && user.getEmail().length() != 254) {
            throw new BadRequestException("Доменная часть адреса электронной почты не может быть больше 63 символов");
        }
    }
}
