package it.paa.resource;

import it.paa.model.dto.EmployeeDTO;
import it.paa.model.dto.EmployeeUpdateDTO;
import it.paa.model.entity.Employee;
import it.paa.model.entity.Role;
import it.paa.service.EmployeeService;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Path("/employees")
public class EmployeeResource {

    @Inject
    EmployeeService employeeService;

    @GET
    public Response getAll(@QueryParam("surname") String surname, @QueryParam("start date") String startDateString, @QueryParam("end date") String endDateString) {
        try {
            LocalDate startDate = null;
            LocalDate endDate = null;

            if (startDateString != null) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    startDate = LocalDate.parse(startDateString, formatter);
                } catch (DateTimeParseException e) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        startDate = LocalDate.parse(startDateString, formatter);

                    } catch (DateTimeParseException ex) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .type(MediaType.TEXT_PLAIN)
                                .entity("start_date: Invalid date format")
                                .build();
                    }
                }
            }

            //check per fare in modo che la data può essere messa in entrambi i modi
            if (endDateString != null) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    endDate = LocalDate.parse(endDateString, formatter);
                } catch (DateTimeParseException e) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        endDate = LocalDate.parse(endDateString, formatter);
                    } catch (DateTimeParseException ex) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .type(MediaType.TEXT_PLAIN)
                                .entity("end_date: Invalid date format")
                                .build();
                    }
                }
            }

            List<Employee> employees = employeeService.getAll(surname, startDate, endDate);
            return Response.ok(employees).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        } catch (NoContentException e) {
            return Response.noContent()
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid EmployeeDTO employeeDTO) {
        LocalDate hiringDate = null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            hiringDate = LocalDate.parse(employeeDTO.getHiringDate(), formatter);
        } catch (DateTimeParseException e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                hiringDate = LocalDate.parse(employeeDTO.getHiringDate(), formatter);

            } catch (DateTimeParseException ex) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("hiring_date: Invalid date format")
                        .build();
            }
        }

        Role role;
        try {
            role = employeeService.getRoleByName(employeeDTO.getRoleName());
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }

        Employee employee = new Employee();

        if (role.getMinSalary() == null || role.getMinSalary().equals(0)) {
            if (employeeDTO.getSalary() == null || employeeDTO.getSalary().equals(0))
                employee.setSalary(0);
            else
                employee.setSalary(employeeDTO.getSalary());
        } else {
            if (employeeDTO.getSalary() == null)
                employee.setSalary(role.getMinSalary());
            else if (role.getMinSalary() != 0 && employeeDTO.getSalary() < role.getMinSalary()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("employees's salary cannot be lower than role role's minimum salary")
                        .build();
            } else
                employee.setSalary(employeeDTO.getSalary());

        }

        employee.setName(employeeDTO.getName());
        employee.setSurname(employeeDTO.getSurname());
        employee.setHiringDate(hiringDate);
        employee.setRole(role);

        return Response.status(Response.Status.CREATED)
                .type(MediaType.APPLICATION_JSON)
                .entity(employeeService.save(employee))
                .build();
    }

    @PUT
    @Path("/employee_id/{employee_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("employee_id") Long employee_id, EmployeeUpdateDTO employeeDTO) {

        if(employeeDTO == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        if (employeeDTO.allEmpty())
            return Response.status(Response.Status.NOT_MODIFIED).build();
        try {

            Employee employee = employeeService.getById(employee_id);
            if (employeeDTO.getName() != null && !employeeDTO.getName().isEmpty() && !employeeDTO.getName().isBlank())
                employee.setName(employeeDTO.getName());

            if (employeeDTO.getSurname() != null && !employeeDTO.getSurname().isEmpty() && !employeeDTO.getSurname().isBlank())
                employee.setSurname(employeeDTO.getSurname());

            if (employeeDTO.getHiringDate() != null && !employeeDTO.getHiringDate().isEmpty() && !employeeDTO.getHiringDate().isBlank()) {
                LocalDate hiringDate = null;
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    hiringDate = LocalDate.parse(employeeDTO.getHiringDate(), formatter);
                    employee.setHiringDate(hiringDate);
                } catch (DateTimeParseException e) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        hiringDate = LocalDate.parse(employeeDTO.getHiringDate(), formatter);
                        employee.setHiringDate(hiringDate);
                    } catch (DateTimeParseException ex) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .type(MediaType.TEXT_PLAIN)
                                .entity("hiring_date: Invalid date format")
                                .build();
                    }
                }
            }

            if (employeeDTO.getRoleName() != null && !employeeDTO.getRoleName().isEmpty() && !employeeDTO.getRoleName().isBlank()) {
                try {
                    Role role = employeeService.getRoleByName(employeeDTO.getRoleName());
                    employee.setRole(role);
                } catch (NoResultException e) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .type(MediaType.TEXT_PLAIN)
                            .entity(e.getMessage())
                            .build();
                }
            }

            if (employeeDTO.getSalary() != null) {
                if (employeeDTO.getSalary() < employee.getRole().getMinSalary()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .type(MediaType.TEXT_PLAIN)
                            .entity("employees's salary cannot be lower than role role's minimum salary")
                            .build();
                }
                else
                    employee.setSalary(employeeDTO.getSalary());
            }

            return Response.ok(employeeService.update(employee)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

}
