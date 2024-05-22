package it.paa.resource;

import it.paa.model.dto.employee.EmployeePostDTO;
import it.paa.model.dto.employee.EmployeePutDTO;
import it.paa.model.entity.*;
import it.paa.service.EmployeeService;
import it.paa.util.DateStringParser;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

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
                    startDate = DateStringParser.parse(startDateString);
                } catch (Exception e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .type(MediaType.TEXT_PLAIN)
                            .entity("start date: " + e.getMessage())
                            .build();
                }
            }

            if (endDateString != null) {
                try {
                    startDate = DateStringParser.parse(endDateString);
                } catch (Exception e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .type(MediaType.TEXT_PLAIN)
                            .entity("end date: " + e.getMessage())
                            .build();
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
                    .build();
        }
    }

    @GET
    @Path("/employee_id/{employee_id}")
    public Response getById(@PathParam("employee_id") Long employeeId) {
        try {
            Employee employee = employeeService.getById(employeeId);
            return Response.ok(employee)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/employee_id/{employee_id}/customers")
    public Response getCustomers(@PathParam("employee_id") Long employeeId) {
        try {
            Employee employee = employeeService.getById(employeeId);

            List<Customer> customerList = employee.getCustomerList();

            if (customerList.isEmpty())
                return Response.status(Response.Status.NO_CONTENT)
                        .build();

            return Response.ok(customerList).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/employee_id/{employee_id}/projects")
    public Response getProjects(@PathParam("employee_id") Long employeeId) {
        try {
            Employee employee = employeeService.getById(employeeId);

            Set<Project> projectList = employee.getProjectList();

            if (projectList.isEmpty())
                return Response.status(Response.Status.NO_CONTENT)
                        .build();

            return Response.ok(projectList).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/employee_id/{employee_id}/technologies")
    public Response getTechnologies(@PathParam("employee_id") Long employeeId) {
        try {
            Employee employee = employeeService.getById(employeeId);

            Set<Technology> technologiesList = employee.getTechnologiesList();

            if (technologiesList.isEmpty())
                return Response.status(Response.Status.NO_CONTENT)
                        .build();

            return Response.ok(technologiesList).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid EmployeePostDTO employeeDTO) {
        if (employeeDTO == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        LocalDate hiringDate = null;

        if (employeeDTO.getHiringDate() != null) {
            try {
                hiringDate = DateStringParser.parse(employeeDTO.getHiringDate());
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("hiring date: " + e.getMessage())
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

        if(employeeDTO.getExperienceLevel() == null)
            employee.setExperienceLevel(0);
        else
            employee.setExperienceLevel(employeeDTO.getExperienceLevel());

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

        try {
            return Response.status(Response.Status.CREATED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(employeeService.save(employee))
                    .build();
        } catch (ConstraintViolationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }

    }

    @PUT
    @Path("/employee_id/{employee_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("employee_id") Long employee_id, EmployeePutDTO employeeDTO) {

        if (employeeDTO == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        if (employeeDTO.isAllEmpty())
            return Response.status(Response.Status.NOT_MODIFIED).build();
        try {

            Employee old = employeeService.getById(employee_id);
            if (employeeDTO.getName() != null)
                old.setName(employeeDTO.getName());

            if (employeeDTO.getSurname() != null)
                old.setSurname(employeeDTO.getSurname());

            if (employeeDTO.getHiringDate() != null) {
                LocalDate hiringDate = null;
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    hiringDate = LocalDate.parse(employeeDTO.getHiringDate(), formatter);
                    old.setHiringDate(hiringDate);
                } catch (DateTimeParseException e) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        hiringDate = LocalDate.parse(employeeDTO.getHiringDate(), formatter);
                        old.setHiringDate(hiringDate);
                    } catch (DateTimeParseException ex) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .type(MediaType.TEXT_PLAIN)
                                .entity("hiring_date: Invalid date format")
                                .build();
                    }
                }
            }

            if (employeeDTO.getRoleName() != null) {
                if (employeeDTO.getRoleName().isEmpty() || employeeDTO.getRoleName().isBlank())
                    return Response.status(Response.Status.BAD_REQUEST)
                            .type(MediaType.TEXT_PLAIN)
                            .entity("role cannot be empty")
                            .build();
                try {
                    Role role = employeeService.getRoleByName(employeeDTO.getRoleName());
                    old.setRole(role);
                } catch (NoResultException e) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .type(MediaType.TEXT_PLAIN)
                            .entity(e.getMessage())
                            .build();
                }
            }

            if(employeeDTO.getExperienceLevel() != null)
                old.setExperienceLevel(employeeDTO.getExperienceLevel());

            if (employeeDTO.getSalary() != null) {
                if (employeeDTO.getSalary() < old.getRole().getMinSalary()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .type(MediaType.TEXT_PLAIN)
                            .entity("employees's salary cannot be lower than role role's minimum salary")
                            .build();
                } else
                    old.setSalary(employeeDTO.getSalary());
            }

            try {
                return Response.ok(employeeService.update(old)).build();
            } catch (ConstraintViolationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity(e.getMessage())
                        .build();
            }
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/employee_id/{employee_id}/add_technology/{technology_id}")
    public Response addTechnology(@PathParam("employee_id") Long employeeId, @PathParam("technology_id") Long technologyId) {
        try {
            employeeService.addTechnology(employeeId, technologyId);
            return Response.ok().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/employee_id/{employee_id}/remove_technology/{technology_id}")
    public Response removeTechnology(@PathParam("employee_id") Long employeeId, @PathParam("technology_id") Long technologyId) {
        try {
            employeeService.removeTechnology(employeeId, technologyId);
            return Response.ok().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/employee_id/{employee_id}")
    public Response delete(@PathParam("employee_id") Long employee_id) {
        try {
            employeeService.delete(employee_id);
            return Response.ok().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        } catch (BadRequestException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

}
