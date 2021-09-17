cmake_minimum_required(VERSION 3.10.2)

project("core")

add_library(
        core

        SHARED

        main.cpp)


find_library(
        log-lib

        log)

target_link_libraries(
        core

        ${log-lib})